package com.mosesn.belt

import scala.collection.{mutable, concurrent}
import scala.collection.JavaConverters.{asScalaSetConverter, mapAsScalaConcurrentMapConverter}
import scala.concurrent.Lock
import scala.annotation.tailrec

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.Collections
import java.lang.{Boolean => JBoolean}


trait ConcurrentMultiMap[A, B] extends mutable.MultiMap[A, B] { self: concurrent.Map[A, mutable.Set[B]] =>
  /**
    * The simplest way to make a threadsafe concurrent Set.
    * Feel free to override as long as it's concurrent
    */
  override protected def makeSet: mutable.Set[B] = Collections.newSetFromMap(new ConcurrentHashMap[B, JBoolean]()).asScala
}


/**
  * A concurrent multimap.  It should be used as a mixin.
  * It uses optimistic locking, so it's better for read-heavy situations.
  * However, it has a memory leak, so it's generally not recommended.
  * @author Moses Nakamura
  */
// TODO: testme
trait LeakyConcurrentMultiMap[A, B] extends ConcurrentMultiMap[A, B] { self: concurrent.Map[A, mutable.Set[B]] =>
  @tailrec
  override final def addBinding(key: A, value: B): this.type = get(key) match {
    case Some(set) => {
      set += value
      this
    }
    case None =>
      if (self.putIfAbsent(key, makeSet + value).isDefined) addBinding(key, value) else this
  }

  override final def removeBinding(key: A, value: B): this.type = {
    get(key).foreach(_ -= value) // this is the memory leak--if the set is empty, it still holds on
    this
  }
}

/**
  * A concurrent multimap.  It should be used as a mixin.
  * @author Moses Nakamura
  */
// TODO: testme
trait PessimisticConcurrentMultiMap[A, B] extends ConcurrentMultiMap[A, B] { self: concurrent.Map[A, mutable.Set[B]] =>
  private[this] val lockMap = (new ConcurrentHashMap[A, ReentrantReadWriteLock]()).asScala
  // precondition: if there is a set for a given key, there must be a lock.
  // precondition: if there is no lock for a given key, there cannot be a set.

  @tailrec
  override final def addBinding(key: A, value: B): this.type = {
    val maybeLock = lockMap.get(key).map(_.readLock)
    maybeLock match {
      case Some(lock) => {
        lock.lockInterruptibly()
        lockMap.get(key).map(_.readLock) match {
          case Some(otherLock) => if (lock == otherLock) {
            // we know that this set will not be removed from under our feet while we have this lock
            get(key) match {
              case Some(set) => {
                set += value
                lock.unlock()
                this
              }
              case None => {
                // the set hasn't been set yet, try again!
                lock.unlock()
                addBinding(key, value)
              }
            }
          }
          else {
            // the set that we started with is already gone, try again!
            lock.unlock()
            addBinding(key, value)
          }
          case None => {
            lock.unlock()
            lockMap.putIfAbsent(key, new ReentrantReadWriteLock())
            if (self.putIfAbsent(key, makeSet + value).isDefined) addBinding(key, value) else this
          }
        }
      }
      case None =>
        lockMap.putIfAbsent(key, new ReentrantReadWriteLock())
        if (self.putIfAbsent(key, makeSet + value).isDefined) addBinding(key, value) else this
    }
  }

  override final def removeBinding(key: A, value: B): this.type = {
    get(key) match {
      case None =>
      case Some(set) => {
        if (set(value)) {
          val maybeLock = lockMap.get(key).map(_.writeLock)
          maybeLock match {
            case Some(lock) => {
              lock.lockInterruptibly()
              lockMap.get(key).map(_.writeLock) match {
                case Some(otherLock) => if (otherLock == lock) {
                  // we're still talking about the same set
                  val singleSet = makeSet + value
                  if (self.remove(key, singleSet)) {
                    lockMap.remove(key)
                    lock.unlock()
                  }
                  else {
                    set -= value
                    lock.unlock()
                  }
                }
                else {
                  // someone else already removed the set we were inspecting!
                  // at some point in time there was an empty set, our work here is done.
                  lock.unlock()
                }
                case None => {
                  // there should always be a lock if there's a set.
                  // if there isn't a lock, then there is no set.
                  lock.unlock()
                }
              }
            }
            case None => {
              // there should always be a lock if there's a set.
              // if there isn't a lock, then there is no set
            }
          }
        }
      }
    }
    this
  }
}
