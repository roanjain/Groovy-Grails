package in.biteclub

class RecordVersion {

    String name
    String oldValue
    String newValue

    static constraints = {
        name nullable: true
        oldValue nullable: true
        newValue nullable: true
    }
}
