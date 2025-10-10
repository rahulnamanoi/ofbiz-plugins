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
package org.apache.ofbiz.testcustomerfeedback

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.DispatchContext
import org.apache.ofbiz.service.ServiceUtil

/**
 * Services for the testcustomerfeedback component.
 */
class CustomerProductFeedbackServices {

    /**
     * Creates a new customer product feedback record.
     */
    static Map<String, Object> createCustomerProductFeedback(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.delegator
        def security = dctx.security

        GenericValue userLogin = context.userLogin as GenericValue

        String productId = context.productId as String
        String partyId = (context.partyId ?: userLogin?.partyId) as String
        Double rating = context.rating != null ? (context.rating as Double) : null
        String comments = context.comments as String
        Timestamp createdDate = context.createdDate as Timestamp ?: UtilDateTime.nowTimestamp()

        if (!userLogin) {
            return ServiceUtil.returnError('UserLogin is required to submit feedback.')
        }
        if (!productId) {
            return ServiceUtil.returnError('productId is required.')
        }
        if (!partyId) {
            return ServiceUtil.returnError('partyId is required.')
        }
        if (rating == null) {
            return ServiceUtil.returnError('rating is required.')
        }
        if (rating < 0.0d || rating > 5.0d) {
            return ServiceUtil.returnError('rating must be between 0.0 and 5.0.')
        }

        boolean hasCatalogCreate = security?.hasEntityPermission('CATALOG', '_CREATE', userLogin)
        if (!hasCatalogCreate && userLogin.partyId != partyId) {
            return ServiceUtil.returnError('You do not have permission to submit feedback on behalf of this party.')
        }

        String feedbackId = delegator.getNextSeqId('CustomerProductFeedback')
        Map<String, Object> fields = [
            customerProductFeedbackId: feedbackId,
            productId                : productId,
            partyId                  : partyId,
            rating                   : rating,
            comments                 : comments,
            createdDate              : createdDate
        ]

        try {
            GenericValue newValue = delegator.makeValue('CustomerProductFeedback', fields)
            newValue.create()
        } catch (Exception e) {
            return ServiceUtil.returnError(e.message)
        }

        Map<String, Object> result = ServiceUtil.returnSuccess()
        result.customerProductFeedbackId = feedbackId
        return result
    }
}
