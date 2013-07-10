package com.mosesn.belt

import scala.collection.{mutable, concurrent}
import scala.collection.JavaConverters.asScalaSetConverter
import scala.annotation.tailrec

import java.util.concurrent.ConcurrentHashMap
import java.util.Collections
import java.lang.{Boolean => JBoolean}

/**
  * A concurrent multimap.  It should be used as a mixin.
  * It uses optimistic locking, so it's better for read-heavy situations.
  * @author Moses Nakamura
  */
// TODO: testme
trait ConcurrentMultiMap[A, B] extends mutable.MultiMap[A, B] { self: concurrent.Map[A, mutable.Set[B]] =>

  /**
    * The simplest way to make a threadsafe concurrent Set.
    * Feel free to override as long as it's concurrent
    */
  override protected def makeSet: mutable.Set[B] = Collections.newSetFromMap(new ConcurrentHashMap[B, JBoolean]()).asScala

  @tailrec
  override final def addBinding(key: A, value: B): this.type = get(key) match {
    case Some(set) => {
      set += value
      this
    }
    case None =>
      if (self.putIfAbsent(key, makeSet + value).isDefined) addBinding(key, value) else this
  }

  @tailrec
  override final def removeBinding(key: A, value: B): this.type = {
    get(key) match {
      case None => this
      case Some(set) => {
        val singleSet = makeSet + value
        if (set == singleSet) {
          if (self.remove(key, singleSet))
            this
          else
            removeBinding(key, value)
        }
        else {
          set -= value
          this
        }
      }
    }
  }
}
