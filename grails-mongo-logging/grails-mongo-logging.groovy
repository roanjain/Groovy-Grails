
    def beforeValidate() {
        boolean loggingEnabled = false
        try {
            if(loggingEnabled) {
                println "Logging changes for class : " + this.getClass().getName()
            }
            if (this.getPersistentValue('id')) {
                if (!hasBeforeUpdate) {
                    hasBeforeUpdate = true
                    DatabaseEvent.withTransaction { status ->
                        List<RecordVersion> recordVersionList = new ArrayList<RecordVersion>()
                        // // println "----- Logging Event Start-----"
                        def d = new DefaultGrailsDomainClass(this.class)
                        def persistentProperties = d.persistentProperties.findAll { it.association == true }.collect {
                            ['name': it.name, 'manyToMany': it.manyToMany, 'type': it.type, 'manyToOne': it.manyToOne]
                        }

                        // println "----- Logging Association Properties -----"
                        Map mapofProperties = [:]
                        /**
                         * Old state of object from hibernate session
                         */
                        if (this.properties['dbo']) {
                            mapofProperties = this.properties['dbo'].toMap()
                        }

                        if(loggingEnabled) {
                            println "mapofProperties" + mapofProperties
                        }

                        this.properties.each { associationProperty ->
                            /**
                             * Changes in collection association objects
                             */
                            if (persistentProperties.findAll {
                                it['name'] == associationProperty.key && it['manyToMany'] == false && (it['type'].toString().contains("java.util.List") || it['type'].toString().contains("java.util.Set"))
                            }) {

                                /**
                                 * Logging addition and deletion of objects
                                 */

                                boolean logKey = false // To prevent the logging of objects does not have Ids

                                if(loggingEnabled) {
                                    println "---Changes in collection association objects----"
                                    println "---Logging addition and deletion of objects-----"
                                    println ""
                                    println "associationProperty.key" + associationProperty.key
                                }

                                def oldIds = mapofProperties.get(associationProperty.key)

                                def Ids = []
                                this."$associationProperty.key".each { associationObject ->
                                    Ids.add((String) associationObject.id)
                                }
                                List<String> oldIdsList = new ArrayList<String>();
                                for (Object el : oldIds) {
                                    if(el) {
                                        if (el?.getClass()?.getSimpleName() == "BasicDBObject") {
                                            if (el['_id']) {
                                                logKey = true
                                                oldIdsList.add((String) el['_id'])
                                            } else {
                                                //oldIdsList.add((String) el)
                                            }
                                        }
                                        if (el?.getClass()?.getSimpleName() == "ObjectId") {
                                            logKey = true
                                            if (el) {
                                                oldIdsList.add((String) el)
                                            }
                                        }
                                    }
                                }
                                oldIdsList = oldIdsList.findAll{it!=null }.unique()
                                Ids = Ids.findAll{it!=null}.unique()

                                if(loggingEnabled){
                                    println "oldIdsList "+oldIdsList
                                    println "newIdsList" +Ids
                                }
                                if(oldIdsList?.size()==0 && Ids?.size()!=0){
                                    logKey = true
                                    persistentProperties.each{
                                        if(it.name == associationProperty.key){
                                            if(it.type.toString().contains("Set") ){
                                                logKey = false
                                            }
                                        }
                                    }
                                }

                                if(logKey) {
                                    if (oldIdsList.size() != (Ids.size())) {
                                        RecordVersion recordVersion = new RecordVersion()
                                        recordVersion.name = associationProperty.key
                                        recordVersion.oldValue = StringUtils.join(oldIdsList, ',');
                                        recordVersion.newValue = StringUtils.join(Ids, ',');
                                        recordVersionList.add(recordVersion)
                                    }
                                }

                                /**
                                 * Logging updation of objects
                                 */

                                if(loggingEnabled){
                                    println "XXXX----------------Logging updation of objects--------------XXXX "
                                }
                                this."$associationProperty.key".each { associationObject ->
                                    def oldDataMap = associationObject.properties['dbo']
                                    if(loggingEnabled) {
                                        println "associationObject" + associationObject
                                        println "oldDataMap " + oldDataMap
                                    }

                                    if(!oldDataMap){
                                        boolean oldObjectNotFound = true
                                        def oldDataMapList = mapofProperties.get(associationProperty.key)
                                        for (Object el : oldDataMapList) {
                                            if(oldObjectNotFound) {
                                                if (el) {
                                                    if (el?.getClass()?.getSimpleName() == "BasicDBObject") {
                                                        if (el['_id']) {
                                                            if (el['_id'] == associationObject.id) {
                                                                oldDataMap = el
                                                                oldObjectNotFound = false
                                                            } else {
                                                                oldDataMap = null
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (oldDataMap) {
                                        associationObject.properties.each {
                                            RecordVersion recordVersion = new RecordVersion()

                                            if(loggingEnabled) {
                                                println "Name : " + it.key
                                                println "Old Value :  " + oldDataMap[it.key]
                                                println "New Value : " + it.value
                                            }


                                            if (!it.value.toString().contains("in.biteclub")) {
                                                if(loggingEnabled ) {
                                                    println "---------------Logging atomic property in association collection list----------------"+it.key
                                                }
                                                if (oldDataMap) {
                                                    if (oldDataMap[it.key]) {
                                                        if (!oldDataMap[it.key].equals(it.value)) {
                                                            if(it?.value?.getClass()?.getSimpleName() == "DateTime"){

                                                                DateTime jodaDate = new DateTime(new Date(oldDataMap[it.key]['time'])).withZone(DateTimeZone.UTC).toDateTime(DateTimeZone.UTC)
                                                                if(loggingEnabled){
                                                                    println "oldJodaDate"+it.value
                                                                    println "newJodaDate"+jodaDate
                                                                }
                                                                if(!jodaDate.equals(it.value)){
                                                                    recordVersion.name = associationObject?.getClass()?.getSimpleName() + "(" + associationObject.id + ")" + it.key
                                                                    recordVersion.newValue = it.value
                                                                    recordVersion.oldValue = jodaDate
                                                                    recordVersionList.add(recordVersion)
                                                                }
                                                            }
                                                            else {
                                                                recordVersion.name = associationObject?.getClass()?.getSimpleName() + "(" + associationObject.id + ")" + it.key
                                                                recordVersion.newValue = it.value
                                                                recordVersion.oldValue = oldDataMap[it.key]
                                                                recordVersionList.add(recordVersion)
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {

                                                if (!it.value.toString().contains("Service") && !it.value.toString().contains("unsaved")){
                                                    /**
                                                     * Skipping the circular checking of nested association object
                                                     */
                                                    /*if(oldDataMap[it.key].getClass().getSimpleName() == "BasicDBList") {
                                                        def oldChildIds = oldDataMap[it.key]
                                                        List<String> oldIdsChildList = new ArrayList<String>();
                                                        for(Object el:oldChildIds){
                                                            if(el.getClass().getSimpleName() == "BasicDBObject") {
                                                                if (el['_id']) {
                                                                    oldIdsChildList.add((String) el['_id'])
                                                                } else {
                                                                    oldIdsChildList.add((String) el)
                                                                }
                                                            }
                                                            if(el.getClass().getSimpleName() == "ObjectId"){
                                                                if (el) {
                                                                    oldIdsChildList.add((String) el)
                                                                }
                                                            }
                                                        }
                                                        oldIdsChildList = oldIdsChildList.unique()
                                                    }
                                                    else {
                                                    */
                                                    if((oldDataMap[it.key]?.getClass()?.getSimpleName() != "BasicDBList") && ((it.value?.getClass()?.getSimpleName()?.toString()?.contains("Set") != true) && (it.value?.getClass()?.getSimpleName()?.toString()?.contains("List") != true) )) {
                                                        if(loggingEnabled ) {
                                                            println "---------------Logging association property in association collection list----------------"+it.key
                                                        }
                                                        String[] token = it.value.toString().split(":")
                                                        String newValue = token[1].toString().trim()
                                                        String oldValue = oldDataMap[it.key].toString().trim()
                                                        if(loggingEnabled) {
                                                            println "Loggin non collection association property"
                                                            println "newValue" + newValue
                                                            println "oldValue " + oldValue
                                                        }
                                                        if (newValue != oldValue) {
                                                            recordVersion.name = associationObject?.getClass()?.getSimpleName() + "(" + associationObject.id + ")" + it.key
                                                            recordVersion.newValue = newValue
                                                            recordVersion.oldValue = oldValue
                                                            recordVersionList.add(recordVersion)
                                                        }
                                                    }
                                                    //}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            /**
                             * Logging non collection association objects
                             */
                            if (persistentProperties.findAll {
                                it['name'] == associationProperty.key && it['type'].toString().contains("in.biteclub") && it['manyToOne'] == true
                            }) {
                                def el = mapofProperties.get(associationProperty.key)
                                def oldId
                                def newId = this."$associationProperty.key"?.id

                                if(el) {
                                    if (el?.getClass()?.getSimpleName() == "BasicDBObject") {
                                        if (el['_id']) {
                                            oldId = el['_id']
                                        }
                                    }
                                    if (el?.getClass()?.getSimpleName() == "ObjectId") {
                                        if (el) {
                                            oldId = el
                                        }
                                    }
                                }
                                if(loggingEnabled) {
                                    println newId
                                    println oldId
                                    println "oldId != newId" + oldId != newId
                                }
                                if(loggingEnabled){
                                    println "------------Logging non collection association objects------------"+associationProperty.key
                                }
                                if(!this."$associationProperty.key"){
                                    RecordVersion recordVersion = new RecordVersion()
                                    recordVersion.name = associationProperty.key
                                    recordVersion.newValue = "null"
                                    recordVersion.oldValue = mapofProperties.get(associationProperty.key)
                                    recordVersionList.add(recordVersion)
                                }
                                else if(oldId != newId){
                                    RecordVersion recordVersion = new RecordVersion()
                                    recordVersion.name = associationProperty.key
                                    recordVersion.newValue = newId
                                    recordVersion.oldValue = oldId
                                    recordVersionList.add(recordVersion)
                                }else {
                                    def associationObject = this."$associationProperty.key"
                                    def oldDataMap = associationObject.properties['dbo']
                                    def d1 = new DefaultGrailsDomainClass(associationObject.class)
                                    def persistentProperties1 = d1.persistentProperties.findAll {
                                        it.association == true
                                    }.collect {
                                        ['name': it.name, 'manyToMany': it.manyToMany, 'type': it.type, 'manyToOne': it.manyToOne, 'bidirectional': it.bidirectional]
                                    }
                                    if(!oldDataMap){
                                        def oldDataMap1 = mapofProperties.get(associationProperty.key)
                                        if(oldDataMap1) {
                                            if (oldDataMap1?.getClass()?.getSimpleName() == "BasicDBObject") {
                                                if (oldDataMap1['_id']) {
                                                    if(oldDataMap1['_id']==associationObject.id){
                                                        oldDataMap = oldDataMap1
                                                    }
                                                    else{
                                                        oldDataMap = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (oldDataMap) {
                                        associationObject.properties.each {
                                            if (persistentProperties1.findAll {
                                                it['name'] == associationProperty.key && it['bidirectional'] != true
                                            }) {
                                                RecordVersion recordVersion = new RecordVersion()

                                                if(loggingEnabled) {
                                                    println "Name : " + it.key
                                                    println "Old Value :  " + oldDataMap[it.key]
                                                    println "New Value : " + it.value
                                                }


                                                if (!it.value.toString().contains("in.biteclub")) {
                                                    if (oldDataMap) {
                                                        if (oldDataMap[it.key]) {
                                                            String oldValue = oldDataMap[it.key]
                                                            String newValue = it.value

                                                            if(it?.value?.getClass()?.getSimpleName() == "DateTime"){

                                                                DateTime jodaDate = new DateTime(new Date(oldDataMap[it.key]['time'])).withZone(DateTimeZone.UTC).toDateTime(DateTimeZone.UTC)
                                                                if(loggingEnabled){
                                                                    println "oldJodaDate"+it.value
                                                                    println "newJodaDate"+jodaDate
                                                                }
                                                                if(!jodaDate.equals(it.value)){
                                                                    recordVersion.name = associationObject?.getClass()?.getSimpleName() + "(" + associationObject.id + ")" + it.key
                                                                    recordVersion.newValue = it.value
                                                                    recordVersion.oldValue = jodaDate
                                                                    recordVersionList.add(recordVersion)
                                                                }
                                                            } else {
                                                                if ((!oldValue.equals(newValue))) {
                                                                    recordVersion.name = associationObject?.getClass()?.getSimpleName() + "(" + associationObject.id + ")" + it.key
                                                                    recordVersion.newValue = it.value
                                                                    recordVersion.oldValue = oldDataMap[it.key]
                                                                    recordVersionList.add(recordVersion)
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (!it.value.toString().contains("Service") && !it.value.toString().contains("unsaved")) {
                                                        String[] token = it.value.toString().split(":")
                                                        String newValue = token[1].toString().trim()
                                                        String oldValue = oldDataMap[it.key].toString().trim()
                                                        if (newValue != oldValue) {
                                                            recordVersion.name = associationObject?.getClass()?.getSimpleName() + "(" + associationObject.id + ")" + it.key
                                                            recordVersion.newValue = newValue
                                                            recordVersion.oldValue = oldValue
                                                            recordVersionList.add(recordVersion)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            /**
                             *  Logging Addition deletion of manyToMany relations
                             */
                            if (persistentProperties.findAll {
                                it['name'] == associationProperty.key && it['manyToMany'] == true
                            }) {

                                if (this.properties['dbo']) {
                                    if(loggingEnabled){
                                        println "-------Logging Addition deletion of manyToMany relations-------"+associationProperty.key
                                    }
                                    def manyToManyOldIds = mapofProperties.get(associationProperty.key + '_$$manyToManyIds')
                                    def manyToManyIds = []
                                    this."$associationProperty.key".each { associationObject ->
                                        manyToManyIds.add((String) associationObject.id)
                                    }
                                    List<String> manyToManyOldIdsList = new ArrayList<String>();
                                    for (Object el : manyToManyOldIds) {
                                        manyToManyOldIdsList.add((String) el)
                                    }
                                    String oldValue = StringUtils.join(manyToManyOldIdsList.unique(), ',')
                                    String newValue = StringUtils.join(manyToManyIds.unique(), ',')
                                    if(loggingEnabled) {
                                        println "oldValue"+oldValue
                                        println "newValue"+newValue
                                    }
                                    if (!oldValue.equals(newValue)) {
                                        RecordVersion recordVersion = new RecordVersion()
                                        recordVersion.name = associationProperty.key
                                        recordVersion.oldValue = oldValue
                                        recordVersion.newValue = newValue
                                        recordVersionList.add(recordVersion)
                                    }

                                } else {
                                    println 'dbo not found for this-->' + this
                                }
                            }
                        }

                        // println "----- Logging Atomic Properties -----"

                        def dirtyPropertyNames = this.dirtyPropertyNames
                        def classProperties = [this.getClass().getName(),'springSecurityService','redisService']
                        def dirtyProperties = dirtyPropertyNames - classProperties

                        if(loggingEnabled) {
                            println "dirtyProperties" + dirtyProperties
                        }

                        dirtyProperties?.collect { name ->
                            RecordVersion recordVersion = new RecordVersion()
                            def originalValue = this.getPersistentValue(name)
                            def newValue = this."$name"

                            if(this."$name"?.getClass()?.getSimpleName() == "DateTime") {
                                DateTime oldDate = new DateTime(this.getPersistentValue(name)).withZone(DateTimeZone.getDefault()).toDateTime(DateTimeZone.getDefault())
                                if(!oldDate.equals(this."$name")){
                                    recordVersion.name = name
                                    recordVersion.oldValue = oldDate
                                    recordVersion.newValue = newValue
                                    recordVersionList.add(recordVersion)
                                    if (loggingEnabled) {
                                        println "$name : old:: $originalValue , new:: $newValue ."
                                    }
                                }
                            }else {

                                if (!originalValue.equals(newValue)) {
                                    recordVersion.name = name
                                    recordVersion.oldValue = originalValue
                                    recordVersion.newValue = newValue
                                    recordVersionList.add(recordVersion)
                                    if (loggingEnabled) {
                                        println "$name : old:: $originalValue , new:: $newValue ."
                                    }
                                }
                            }
                        }
                        def dbEvent = new DatabaseEvent(loggedInUser: springSecurityService?.getCurrentUser() ?: null,
                                type: "Updated", entityClass: this?.getClass()?.getSimpleName(), eventObjectId: this.id, changedProperties: recordVersionList)
                        dbEvent.save(failOnError: true)
                    }
                }
            }
        }catch (Exception e){
            println "Exception in logging with cause -->"+e.getMessage()
        }
    }

}


    def afterInsert() {
            // println "Inserting..."
            if (!hasAfterInsert) {
                // println "checking==>"
                hasAfterInsert = true
                DatabaseEvent.withTransaction { status ->
                    /**
                     * The code below in block is a  workaround to prevent mongo using the id again, that is
                     * already saved for central Dish object, need to be fixed
                     */
                    // --- Start --
                    this.delete()
                    beforeDelete = true
                    def dbEvent = new DatabaseEvent(loggedInUser: springSecurityService?.getCurrentUser(),
                            type: "Created", entityClass: this.getClass().getSimpleName(), eventObjectId: this.id)
                    dbEvent.save(failOnError: true)
                }
            }
        }catch (Exception e){
            println "Exception in logging with cause -->"+e.getMessage()
        }
    }


    def beforeDelete() {
            // println "Deleteing..."
            println this.getClass().getName()
            if (!beforeDelete) {
                beforeDelete = true
                DatabaseEvent.withTransaction { status ->
                    def dbEvent = new DatabaseEvent(loggedInUser: springSecurityService.getCurrentUser(),
                            type: "Deleted", entityClass: this.getClass().getSimpleName(), eventObjectId: this.id)
                    dbEvent.save(failOnError: true)
                }
            }
        }catch (Exception e){
            println "Exception in logging with cause -->"+e.getMessage()
        }
    }

