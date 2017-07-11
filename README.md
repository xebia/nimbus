# Nimbus 
Nimbus is a Akka HTTP powered client for Google Datastore. It uses the `connectionPool` implementation of Akka HTTP to 
ensure optimal running performance with a small footprint. 
 
The client consists of two seperate layers: 
1. A _raw_ layer which is as less opinionated as possible, translating the REST specs and its objects into a pluggable, 
stackable and type-safe solution for communication with Google Datastore. All traits delivering this functionality
can be found in the `RawClient` class.
2. A _opinionated_ layer which abstracts models and API calls into a more friendly and usable whole. It is within this
 layer where most development will be done to ensure a developer-friendly / batteries included 
 solution for communication with Google Datastore.
  
## Current state
In its current state, Nimbus should be treated as Alpha software. The client isn't feature complete yet, 
the structure and DSL can change and heavy testing in production is still to be done. However, in its basis and in 
context of the technology powering the clien; most parts of the client should be stable for test usage
 
### Currently available
- Opinionated (Nimbus) layer for comfortable usage of Google Datastore's functionality. 
- Raw layer for more direct communication with Google Datastore.
- Reactive (OAuth) authentication layer for automatic retrieval of access tokens when required. 


### Currently missing / soon to be added
- (Typesafe / Lightbend) based configuration for (authentication) parameters. 
- GQL implementation within opinionated layer for querying (idea is to make this compiler checked)
- Excessive test suite for all functionality (most is covered in the Nimbus test) and access token retrieval (should be mocked).
- Benchmark tests for performance calibration. 
- Way of switching test suite between emulated server and actual server (currently all testing is done through the emulated layer)
- More robust Nimbus Test trait for inclusion in projects which want a mocked, emulated or production-running version 
of the client during tests.   

## Usage
The `RawClient` can be initialized by a `projectId` and a `Credentials` instance. The `Credentials` class consists of a
email address and a private key. These can be constructed any way the user desires, though it's easiest to construct
these through the functionality available within the `OAuthApi` object:

```scala
def readCredentialsFromFile(file: File): Credentials

def readCredentialsFromEnvironment(): Credentials 
```

When the `readCredentialsFromEnvironment()` method is used, the credentials (note: not the `PK12` but the `json` variant) 
will be read from the file defined in the `GOOGLE_APPLICATION_CREDENTIALS` environment variable. 
 
Using these credentials, the `RawClient` can be initialized:

```scala
RawClient(readCredentialsFromEnvironment(), "your_project_id")
```

### Raw functionality
Upon initialization, the `RawClient` will automatically generate its connection pool and authentication layer, 
and calls are ready to be done towards Google Datastore: 

```scala
val entities = List(
    RawEntity(Key.named(client.projectId, "$TestObject", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
    RawEntity(Key.named(client.projectId, "$TestObject", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
)

val mutations = entities.map(Insert.apply)
val keys = entities.map(_.key)

for {
  transactionId <- client.beginTransaction()
  _ <- client.commit(Some(transactionId), mutations, CommitMode.Transactional)
  lookup <- client.lookup(ExplicitConsistency(ReadConsistency.Eventual), keys)
} yield lookup
```

For the coverage of the rest of the _raw_ functionality, it's best to check the test suite.

### DSL
The opinionated layer can be initialized by either passing along the namespace of your objects and a already initialized 
client (when you want to recycle a client over multiple namespaces): 

```scala
val nimbus = Nimbus(namespace, client)
```

Or by passing the credentials directly: 
 
```scala 
val nimbus = Nimbus(credentials, projectId, namespace)
```

As additional parameters, both a `OverflowStrategy` can be supplied as a manner of back-pressure strategy and a 
`maximumRequestsInFlight` parameter which states how many requests can be _unhandled_ until the back-pressure strategy is used.
 Per default, a `OverflowStrategy.backpressure` strategy is used, combined with a max-in-flight of `1024`.

#### Consistency levels
All procedures done to the Google Datastore can be either done using a _Transaction Id_ or by setting the consistency level
to either _Eventual_ or _Strong_. For each of the functions described below (and all other available in DSL), a counter part
for each of these levels is to be found. The short-hand functions default to an _eventual consistency_ level. 

#### Entities and paths
The created DSL / client is able to write and read objects which have a `EntityConverter[A]` type class implemented, or 
can directly use the `Entity` class as a pass-through.

The Entity:
```scala
final case class Entity(path: Path, properties: Map[String, Value])
```

Is a class which has a `path` and a set of `properties`. The path is a abstraction over the default `Key` structure
available within Google Datastore, and uses the defined namespace within the Nimbus client / DSL to ensure easier creation
and handling of these keys. In the `properties`, the actual value is contained which is eventually stored into Google
 Datastore. The set of available types in Google Datastore is rich enough to translate most data classes within applications and
 implicits are available to transform the basic Scala types to and back from Google Datastore `Values`: 

```scala 
import com.xebia.nimbus.Path._
import com.xebia.nimbus.datastore.model.Value._

case class Person(name: String, age: Int)

implicit val personEntityFormatter = new EntityConverter[Person] {
    override def write(p: Person): Entity = Entity('Person, p.name, Map("name" -> p.name, "age" -> p.age))

    override def read(entity: Entity): Person = Person(entity.properties("name").as[String], entity.properties("age").as[Int])
}

```

Paths are automatically transformed to keys and can be nested to create tree / directory like structures: 
  
```scala
('Person -> "Bob") / ('Children -> "Mike")
('Account -> 577321) / 'Transaction
```

Every path consists of the _kind_ of an entity on the left side and the `name` (`String`) or `id` (`long`) on the right side. 
When objects are stored which only define a _kind_ but not a `name` or `id`, a `identifier` is generated automatically 
by Google Datastore.

#### CRUD
Using either a serializable case class (one for which a formatter is defined as above), or direct usage of a `Entity`, 
objects can be inserted, upserted, updated and deleted into and from the database:


```scala
val mike = Person("Mike", 8)
val nikky = Person("Nikky", 12)
val bob = Person("Bob", 48)

"Nimbus basic DSL" should {
"correctly store objects" in {
    for {
        _ <- nimbus.insert(Seq(mike, nikky, bob))
        _ <- nimbus.delete('Person -> bob.name)
        _ <- nimbus.update(Seq(mike, nikky))
        _ <- nimbus.upsert(Entity('Person, "Bob", Map("name" -> "Bob", "age" -> 48)))
    } yield {}
}
```

#### Lookup

Stored items can be looked-up using the look-up API:
```scala
for {
    m <- nimbus.lookup[Person]('Person -> mike.name)
    b <- nimbus.lookup[Entity]('Person -> "Bob")
} yield {
    m.get.age shouldBe 8
    b.get.properties("age") shouldBe 48
}
```

#### Querying

Besides the look-up functionality, more extensive querying can be done using the _query_ API: 

```scala
import com.xebia.nimbus.Query._
for {
    _ <- nimbus.upsert(Seq(mike, nikky, bob))
    q <- nimbus.query[Person](Q.kindOf('Person).filterBy('age > 6))
    q2 <- nimbus.query[Person](Q.kindOf('Person).filterBy('age > 6 and 'age < 20))
    q3 <- nimbus.query[Person](Q.kindOf('Person).filterBy('age > 6 and 'age < 20 and 'age > 10))
    q4 <- nimbus.querySource[Person](Q.kindOf('Person).filterBy('age > 6)).runWith(Sink.seq)
} yield {
    q.results should contain theSameElementsAs Seq(mike, nikky, bob)
    q2.results should contain theSameElementsAs Seq(mike, nikky)
    q3.results should contain theSameElementsAs Seq(nikky)
}
```

The query DSL exposes multiple functions which are used to build a query: 

```scala
def kindOf(kind: Symbol): QueryDSL

def orderAscBy(field: Symbol): QueryDSL

def orderDescBy(field: Symbol): QueryDSL

def filterBy(filter: Filter): QueryDSL

def projectOn(fields: Symbol*): QueryDSL

def startFrom(cursor: String): QueryDSL

def endAt(cursor: String): QueryDSL

def withOffset(offset: Int): QueryDSL

def withLimit(limit: Int): QueryDSL
```

## Running tests
The test suite expects that the Google Cloud Datastore emulator is running on port `8080`, the following command can be 
run to start the emulator (the Google Cloud tools should be installed):

``` 
gcloud beta emulators datastore start --host-port localhost:8080 --consistency 1.0 --project nimbus-test --data-dir project-test
```