/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.ofbiz.base.crypto.HashCrypt
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.service.ServiceUtil

def createUserAccount() {
    def requiredFields = ["firstName", "lastName", "email", "username", "password"]
    for (String field : requiredFields) {
        if (UtilValidate.isEmpty(context[field])) {
            return ServiceUtil.returnError("${field} is required.")
        }
    }

    String username = context.username.trim()
    String email = context.email.trim()

    if (UtilValidate.isEmpty(username)) {
        return ServiceUtil.returnError("username is required.")
    }

    if (!UtilValidate.isEmail(email)) {
        return ServiceUtil.returnError("A valid email address is required.")
    }

    def existingLogin = delegator.findOne("UserLogin", [userLoginId: username], false)
    if (existingLogin) {
        return ServiceUtil.returnError("UserLogin with id ${username} already exists.")
    }

    def partyId = delegator.getNextSeqId("Party")
    def nowTimestamp = UtilDateTime.nowTimestamp()

    try {
        delegator.create("Party", [
                partyId     : partyId,
                partyTypeId : "PERSON",
                statusId    : "PARTY_ENABLED",
                createdDate : nowTimestamp,
                lastModifiedDate: nowTimestamp
        ])

        delegator.create("Person", [
                partyId   : partyId,
                firstName : context.firstName.trim(),
                lastName  : context.lastName.trim()
        ])

        delegator.create("PartyRole", [
                partyId    : partyId,
                roleTypeId : "CUSTOMER"
        ])

        def contactMechId = delegator.getNextSeqId("ContactMech")
        delegator.create("ContactMech", [
                contactMechId     : contactMechId,
                contactMechTypeId : "EMAIL_ADDRESS",
                infoString        : email
        ])

        delegator.create("PartyContactMech", [
                partyId        : partyId,
                contactMechId  : contactMechId,
                contactMechPurposeTypeId: "PRIMARY_EMAIL",
                fromDate       : nowTimestamp
        ])

        def hashedPassword = HashCrypt.cryptUTF8("SHA", null, context.password)

        delegator.create("UserLogin", [
                userLoginId           : username,
                partyId               : partyId,
                currentPassword       : hashedPassword,
                enabled               : "Y",
                hasLoggedOut          : "N",
                disabledBy            : null,
                requirePasswordChange : "N"
        ])

        def dispatchContext = dispatcher?.getDispatchContext()
        if (dispatchContext && dispatchContext.getModelService("createUserLoginPassword")) {
            dispatcher.runSync("createUserLoginPassword", [
                    userLoginId            : username,
                    currentPassword        : context.password,
                    currentPasswordVerify  : context.password,
                    requirePasswordChange  : "N"
            ])
        }

        delegator.create("UserLoginSecurityGroup", [
                userLoginId : username,
                groupId     : "CUSTOMER",
                fromDate    : nowTimestamp
        ])

        def result = ServiceUtil.returnSuccess("User ${username} created successfully!")
        result.userLoginId = username
        result.successMessage = "User ${username} created successfully!"
        return result
    } catch (Exception e) {
        return ServiceUtil.returnError(e.message ?: e.toString())
    }
}
