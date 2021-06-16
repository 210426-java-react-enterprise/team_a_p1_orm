# Alpha Team ORM
Team Alpha's ORM repo
Associated Web App can be found here: https://github.com/210426-java-react-enterprise/team_a_p1_webapp

# General
Custom ORM used to abstract away boiler plate JDBC configuration and code. To set up and use the ORM on any application, follow the brief outline below.

## ORM Build
To build as a maven dependency, use `mvn clean package` then `mvn install` to add to local m2. If performed on a live server, this action should be invoked from the web-app build spec.

## Connecting to Database
Currently only PostgreSQL drivers are supported.

To establish a connection with the database, two annotations will need to be used in whichever class will be configuring the connection. After these annotations are provided, all references to the database will be made through a Session object. In order to instantiate the Session object, Session.open() and its accompanying Session.close() once finished should be invoked. All accompanying CRUD actions are also performed through this Session object, for example saving would be Session.save(objectToBeSaved);

### @ConnectionConfig(filepath = ?)

This annotation is optional but reccommended. It accepts a string containing the file path to a .properties file. This is necessary if you do not wish to expose sensitive information to the raw source code. 

### @JDBCConnection

This annotation is mandatory, and accepts several parameters. If wishing to source from .properties file, wrap the property name like so after the equals sign: ${name}
 * url = The full url of the database without any prefix. example: amazonaws.com (no http://). .properties example:  ${host_url}
 * username = Username for connecting to database. example: admin. .properties example: ${username}
 * password = Password for connecting to database. Highly recommended using .properties file. example: ${password}
 * schema = OPTIONAL. Provide a specific schema you wish to reference. example: public. .properties example: ${schema}

## Functionality
Annotations necessary for models:
 * @Table : Class scope. Table name can be provided. By default assumes table name is same as class name.
 * @Entity : Class scope. Slightly redundant. Anything annotated with @Entity should also be @Table and vice versa.
 * @Column : Field scope. All fields that are expected to be in the database should be annotated with @Column. By default the column name is the same as the field name, but one can be provided with name(). Additional functionality: notNull() and unique(). Both are false by default.
 * @Id : Field scope. The primary key for the table. One and only one is required.
 * Unfinished Annotations: @ForeignKey and @Constraints. Currently provide no functionality.

The following CRUD operations are provided by the ORM:
 * Session.find(Class<T> clazz, String fieldName, String fieldValue) : Finds one or more entries matching provided field and value. If one entry is expected, invoke .getFirstEntry(). If a list is expected, invoke .getList(). 
   - clazz: class of model to be built. e.g. User.class
   - fieldName: name of field being searched. e.g. "username"
   - fieldValue: value of field being searched. e.g. "John"
 * Session.findAll(Clazz<T> clazz) : Equivalent: SELECT * FROM table where table is the correctly annotated model class provided.
   - clazz: class of model to be built. e.g. User.class
 * Session.save(Object object) : Saves object provided to database.
   - object: object that conforms to a properly annotated model that is to be updated/saved.
 * Session.insert(Object object) : Creates new entry in database with provided object.
   - object: object to be created that conforms to a properly annotated model.
 * Session.remove(Object object) : Removes an entry from the database conforming to provided model.
   - object: object to be removed that conforms to a properly annotated model.
 * Session.isEntityUnique(Object object) : Checks if entry already exists in DB. Returns true if it does not, false if it does.
   - object: object to check against DB. Must conform to properly annotated model.
  
