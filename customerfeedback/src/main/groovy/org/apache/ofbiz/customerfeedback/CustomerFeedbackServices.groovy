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
package org.apache.ofbiz.customerfeedback

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.service.ServiceUtil

/**
 * Services for the customerfeedback component.
 */
class CustomerFeedbackServices {

    /**
     * Creates a new customer feedback entry.
     */
    static Map<String, Object> createCustomerFeedback() {
        Map<String, Object> result
        String productId = parameters.productId
        String customerId = parameters.customerId
        Double rating = parameters.rating as Double
        String comments = parameters.comments
        Timestamp createdDate = parameters.createdDate as Timestamp ?: UtilDateTime.nowTimestamp()

        if (!productId) {
            return ServiceUtil.returnError("productId is required")
        }
        if (!customerId) {
            return ServiceUtil.returnError("customerId is required")
        }
        if (rating == null) {
            return ServiceUtil.returnError("rating is required")
        }
        if (rating < 0.0d || rating > 5.0d) {
            return ServiceUtil.returnError("rating must be between 0 and 5")
        }

        String feedbackId = delegator.getNextSeqId("CustomerFeedback")
        Map<String, Object> feedbackFields = [
            feedbackId : feedbackId,
            productId  : productId,
            customerId : customerId,
            rating     : rating,
            comments   : comments,
            createdDate: createdDate
        ]
        delegator.create("CustomerFeedback", feedbackFields)

        result = ServiceUtil.returnSuccess()
        result.feedbackId = feedbackId
        return result
    }
}
