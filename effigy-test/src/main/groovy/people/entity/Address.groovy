package people.entity

import groovy.transform.Immutable

@Immutable
class Address {

    String line1
    String line2
    String city
    String state
    String zip
}