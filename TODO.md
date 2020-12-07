# TODO
- [ ] introduce GPL
  - [ ] rename packages
  - [ ] rename github repo
  - [ ] update drone/codecov integration
  - [ ] update readme
  - [ ] create documentation site

- [ ] Diff. Although diff and patch are always comes together, semantic is a little different.
      Whereas Patch is more like a sequence of events (or commands, if you'd like), 
      Diff is a pair of values with detailed explanation on what exactly differs one from another.
- [x] ordered collection diff
- [ ] port `org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch` to Scala ???
      - can win in space if `Equal(text)` would be replaced with `Skip(n)`
- [ ] patch serialization
  - [ ] binary (protobuf?)
  - [ ] json
- [ ] more types
  - [x] java.sql
  - [ ] etc
- [ ] more tests
  - [x] java.sql
- [x] prepare for publishing
  - [x] readme
    - [x] intro with example
  - [x] sbt
  - [x] drone
  - [x] codecov
  - [x] sonatype
- [x] more sophisticated text diff/patch
- [ ] improve readme
  - [ ] api details
  - [ ] adapters
  - [ ] macros
  - [ ] provide Ammonite REPL based walkthrough 
- [ ] provide more space efficient algorithm/structure (detect similar sequences) for ordered collections patch
  