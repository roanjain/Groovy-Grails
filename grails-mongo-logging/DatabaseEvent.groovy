package in.biteclub

import org.bson.types.ObjectId

class DatabaseEvent {

    ObjectId id
    String type
    String entityClass
    String eventObjectId
    Date dateCreated
    User loggedInUser
    List<RecordVersion> changedProperties

    static embedded = ['changedProperties']

    static constraints = {
        eventObjectId nullable: true
        entityClass nullable: true
        loggedInUser nullable: true
        changedProperties nullable : true
    }
}
