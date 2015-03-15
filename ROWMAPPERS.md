> A future feature idea...

How do you create/use them in effigy... something like:

class Mappers {

    static personMapper(){
        mapper(Person){
            ...
        }
    }
}

@SqlSelect('select * from people')
@RowMapper(type=Mappers, factory='personMapper')
abstract List<Person> listAll()

///////


could provide a runtime usage of this to generate row mappers as DSL

coule also provide a compile time use case where done in a special config class to build compiled mappers based on the DSL - compile time might not buy you much here since you can transform

mapper(Person){
    map 'first_name' into 'firstName'
    map 'second_name' into 'middleName'
    map 'last_name' using { v-> v.capitalize() } into 'lastName'

    map 'first_name' into 'name' at 'firstName'
    // map( 'first_name' ).into('name').at('firstName')

    map 'last_name' using {} into 'name' at 'lastName'
    // map('last_name').using({}).into('name').at('lastName')
}

mapProperty 'firstName' - helper for mapping 'first_name' into 'firstName'

map(string) - map field name value
map(index) - map by index

into(string) - property name on target

at(string) - subproperty to set on into target

using - allows conversion of the mapped value, based on the incoming field value

could tie this in using the class+factory


class SomeMapperFactory {

    @RowMapperProvider - something like this to hook into the ast? or can use runtime dsl
    static RowMapper myMapper(){
        mapper(Something){ .. }
    }

    static RowMapper otherMapper(){
        new ConfiguredRowMapper(Something, {...})
    }
}

// runtime version

class ConfiguredRowMapper implements RowMapper {

    def mappings = []

    Person mapRow( rs ){
        Person instance = new Person()
        mappings.each { m->
            m.transfer(rs, instance)
        }
        instance
    }
}

// compile time - the simplest approach does not give much benefit, but if you use the ast method defs as a script in
themselves so that you take

    map 'last_name' using { v-> v.capitalize() } into 'name' at 'lastName'
    map 'last_name' into 'name' at 'lastName'

and use it to build

    instance.person.lastName = { v-> v.capitalize() }.call(rs['last_name])
    instance.person.lastName = rs['last_name]

which could have performance benefits over the simple compile and the runtime version, but would be harder to code in ast
in a pinch it could just dump the mapper code in as is