package com.cloneObject

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClassProperty

class CloneObjectController {

    def index() { }
	
	def deepClone = { objectToClone ->
		
			if (objectToClone.getClass().name.contains("_javassist"))
			return null
			//Our target instance for the instance we want to clone
			def objectToCloneCopy = objectToClone.getClass().newInstance()
			//Returns a DefaultGrailsDomainClass (as interface GrailsDomainClass) for inspecting properties
			def domainClass = objectToClone.domainClass.grailsApplication.getDomainClass(objectToCloneCopy.getClass().name)
			def notCloneable = domainClass.getPropertyValue("notCloneable")
			for(DefaultGrailsDomainClassProperty prop in domainClass?.getPersistentProperties()) {
			if (notCloneable && prop.name in notCloneable)
			continue
			if (prop.association) {
			if (prop.owningSide) {
			// Deep clone owned associations
			if (prop.oneToOne) {
			def newAssociationInstance = deepClone(objectToClone?."${prop.name}")
			objectToCloneCopy."${prop.name}" = newAssociationInstance
			} else {
			objectToClone."${prop.name}".each { associationInstance ->
			def newAssociationInstance = deepClone(associationInstance)
			if (newAssociationInstance)
			objectToCloneCopy."addTo${prop.name.capitalize()}"(newAssociationInstance)
			}
			}
			}
			else {
			if (!prop.bidirectional) {
					if(prop.oneToMany == true){
					objectToClone."${prop.name}".each { associationInstance ->
						def newAssociationInstance = deepClone(associationInstance)
						//fixedPriceCopy."addTo${prop.name.capitalize()}"(newAssociationInstance)
						}
					}
					else{
						objectToCloneCopy."${prop.name}" = objectToClone."${prop.name}"
					}
					
			}
			// Yes bidirectional and not owning. E.g. clone Report, belongsTo Organisation which hasMany
			// manyToOne. Just add to the owning objects collection.
			else {
			if (prop.manyToOne) {
			objectToCloneCopy."${prop.name}" = objectToClone."${prop.name}"
			def owningInstance = objectToClone."${prop.name}"
			// Need to find the collection.
			String otherSide = prop.otherSide.name.capitalize()
			//println otherSide
			//owningInstance."addTo${otherSide}"(newDomainInstance)
			}
			else if (prop.manyToMany) {
			//newDomainInstance."${prop.name}" = [] as Set
			objectToClone."${prop.name}".each {
			//newDomainInstance."${prop.name}".add(it)
			}
			}
			else if (prop.oneToMany) {
			objectToClone."${prop.name}".each { associationInstance ->
			def newAssociationInstance = deepClone(associationInstance)
			objectToCloneCopy."addTo${prop.name.capitalize()}"(newAssociationInstance)
			}
			}
			}
			}
			} else {
			//If the property isn't an association then simply copy the value
			objectToCloneCopy."${prop.name}" = objectToClone."${prop.name}"
			if (prop.name == "dateCreated" || prop.name == "lastUpdated") {
			objectToCloneCopy."${prop.name}" = null
			}
			}
			}
			log.debug objectToCloneCopy
			objectToCloneCopy.save()
			if (!objectToCloneCopy.save()) {
				objectToCloneCopy.errors.each {
					println it
				}
			}
			return objectToCloneCopy
			}
		   
	
}
