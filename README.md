# belt
things I find myself reimplementing over and over and over again

## but aren't these things super trivial?
yes I don't care

## api
```
class AbstractSpec extends FunSpec { self: Named =>
  describe(self.name) {
    it("does something") {
      ...
    }
    ...
  }
}
```
