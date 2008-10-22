/*******************************************************************************
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
 *******************************************************************************/
package org.ofbiz.common.preferences;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import static org.ofbiz.base.util.UtilGenerics.checkMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * User preference services.<p>User preferences are stored as key-value pairs.
 * <p>User preferences can be grouped - so that multiple preference pairs can be
 * handled at once. Preference groups also allow a single userPrefTypeId to be
 * used more than once - with each occurence having a unique userPrefGroupId.</p>
 * <p>User preference values are stored as Strings, so the easiest and most
 * efficient way to handle user preference values is to keep them as strings.
 * This class handles any data conversion needed.</p>
 */
public class PreferenceServices {
    public static final String module = PreferenceServices.class.getName();

    public static final String resource = "PrefErrorUiLabels";

    /**
     * Retrieves a single user preference from persistent storage. Call with
     * userPrefTypeId and optional userLoginId. If userLoginId isn't
     * specified, then the currently logged-in user's userLoginId will be
     * used. The retrieved preference is contained in the <b>userPrefMap</b> element.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getUserPreference(DispatchContext ctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        if (!PreferenceWorker.isValidGetId(ctx, context)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.permissionError", locale));
        }
        GenericDelegator delegator = ctx.getDelegator();

        String userPrefTypeId = (String) context.get("userPrefTypeId");
        if (UtilValidate.isEmpty(userPrefTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.invalidArgument", locale));
        }
        String userLoginId = PreferenceWorker.getUserLoginId(context, true);
        Map<String, String> fieldMap = UtilMisc.toMap("userLoginId", userLoginId, "userPrefTypeId", userPrefTypeId);
        String userPrefGroupId = (String) context.get("userPrefGroupId");
        if (UtilValidate.isNotEmpty(userPrefGroupId)) {
            fieldMap.put("userPrefGroupId", userPrefGroupId);
        }

        Map<String, Object> userPrefMap = null;
        try {
            GenericValue preference = EntityUtil.getFirst(delegator.findByAnd("UserPreference", fieldMap));
            if (preference != null) {
                userPrefMap = PreferenceWorker.createUserPrefMap(preference);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.readFailure", new Object[] { e.getMessage() }, locale));
        } catch (GeneralException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.readFailure", new Object[] { e.getMessage() }, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("userPrefMap", userPrefMap);
        if (userPrefMap != null) {
            // Put the value in the result Map too, makes access easier for calling methods.
            Object userPrefValue = userPrefMap.get(userPrefTypeId);
            if (userPrefValue != null) {
                result.put("userPrefValue", userPrefValue);
            }
        }
        return result;
    }

    /**
     * Retrieves a group of user preferences from persistent storage. Call with
     * userPrefGroupId and optional userLoginId. If userLoginId isn't
     * specified, then the currently logged-in user's userLoginId will be
     * used. The retrieved preferences group is contained in the <b>userPrefMap</b> element.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getUserPreferenceGroup(DispatchContext ctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        if (!PreferenceWorker.isValidGetId(ctx, context)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.permissionError", locale));
        }
        GenericDelegator delegator = ctx.getDelegator();

        String userPrefGroupId = (String) context.get("userPrefGroupId");
        if (UtilValidate.isEmpty(userPrefGroupId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.invalidArgument", locale));
        }
        String userLoginId = PreferenceWorker.getUserLoginId(context, true);

        Map<String, Object> userPrefMap = null;
        try {
            Map<String, String> fieldMap = UtilMisc.toMap("userLoginId", "_NA_", "userPrefGroupId", userPrefGroupId);
            userPrefMap = PreferenceWorker.createUserPrefMap(delegator.findByAnd("UserPreference", fieldMap));
            fieldMap.put("userLoginId", userLoginId);
            userPrefMap.putAll(PreferenceWorker.createUserPrefMap(delegator.findByAnd("UserPreference", fieldMap)));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.readFailure", new Object[] { e.getMessage() }, locale));
        } catch (GeneralException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "getPreference.readFailure", new Object[] { e.getMessage() }, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("userPrefMap", userPrefMap);
        return result;
    }

    /**
     * Stores a single user preference in persistent storage. Call with
     * userPrefTypeId, userPrefGroupId, userPrefValue and optional userLoginId.
     * If userLoginId isn't specified, then the currently logged-in user's
     * userLoginId will be used.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> setUserPreference(DispatchContext ctx, Map<String, ?> context) {
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        String userPrefTypeId = (String) context.get("userPrefTypeId");
        Object userPrefValue = (String) context.get("userPrefValue");
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefTypeId) || userPrefValue == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "setPreference.invalidArgument", locale));
        }
        String userPrefGroupId = (String) context.get("userPrefGroupId");
        String userPrefDataType = (String) context.get("userPrefDataType");

        try {
            if (UtilValidate.isNotEmpty(userPrefDataType)) {
                userPrefValue = ObjectType.simpleTypeConvert(userPrefValue, userPrefDataType, null, null, false);
            }
            GenericValue rec = delegator.makeValidValue("UserPreference", PreferenceWorker.toFieldMap(userLoginId, userPrefTypeId, userPrefGroupId, userPrefValue));
            delegator.createOrStore(rec);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "setPreference.writeFailure", new Object[] { e.getMessage() }, locale));
        } catch (GeneralException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "setPreference.writeFailure", new Object[] { e.getMessage() }, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Stores a user preference group in persistent storage. Call with
     * userPrefMap, userPrefGroupId and optional userLoginId. If userLoginId
     * isn't specified, then the currently logged-in user's userLoginId will be
     * used.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> setUserPreferenceGroup(DispatchContext ctx, Map<String, ?> context) {
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        Map<String, Object> userPrefMap = checkMap(context.get("userPrefMap"), String.class, Object.class);
        String userPrefGroupId = (String) context.get("userPrefGroupId");
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefGroupId) || userPrefMap == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "setPreference.invalidArgument", locale));
        }

        try {
            for (Iterator i = userPrefMap.entrySet().iterator(); i.hasNext();) {
                Map.Entry mapEntry = (Map.Entry) i.next();
                GenericValue rec = delegator.makeValidValue("UserPreference", PreferenceWorker.toFieldMap(userLoginId, (String) mapEntry.getKey(), userPrefGroupId, (String) mapEntry.getValue()));
                delegator.createOrStore(rec);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "setPreference.writeFailure", new Object[] { e.getMessage() }, locale));
        } catch (GeneralException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "setPreference.writeFailure", new Object[] { e.getMessage() }, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Copies a user preference group. Call with
     * fromUserLoginId, userPrefGroupId and optional userLoginId. If userLoginId
     * isn't specified, then the currently logged-in user's userLoginId will be
     * used.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> copyUserPreferenceGroup(DispatchContext ctx, Map<String, ?> context) {
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        String fromUserLoginId = (String) context.get("fromUserLoginId");
        String userPrefGroupId = (String) context.get("userPrefGroupId");
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefGroupId) || UtilValidate.isEmpty(fromUserLoginId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "copyPreference.invalidArgument", locale));
        }

        try {
            Map<String, String> fieldMap = UtilMisc.toMap("userLoginId", fromUserLoginId, "userPrefGroupId", userPrefGroupId);
            List<GenericValue> resultList = delegator.findByAnd("UserPreference", fieldMap);
            if (resultList != null) {
                for (GenericValue preference: resultList) {
                    preference.set("userLoginId", userLoginId);
                }
                delegator.storeAll(resultList);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "copyPreference.writeFailure", new Object[] { e.getMessage() }, locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
