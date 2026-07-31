/*
  Copyright (c) Lightstreamer Srl

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.lightstreamer.examples.chat_demo.adapters;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.metadata.CreditsException;
import com.lightstreamer.adapter.metadata.LoginInfo;
import com.lightstreamer.adapter.metadata.MessageException;
import com.lightstreamer.adapter.metadata.MetadataAdapter;
import com.lightstreamer.adapter.metadata.SessionListener;
import com.lightstreamer.adapter.metadata.SessionRequestHandler;
import com.lightstreamer.adapter.metadata.SubscriptionHandler;
import com.lightstreamer.adapter.metadata.SubscriptionInfo;
import com.lightstreamer.adapter.metadata.mpn.MPNActivationHandler;
import com.lightstreamer.adapter.metadata.mpn.MPNActivationInfo;
import com.lightstreamer.adapter.metadata.mpn.MPNDeviceInfo;
import com.lightstreamer.adapter.metadata.mpn.MPNPlatformType;
import com.lightstreamer.adapter.metadata.mpn.MPNSubscriptionInfo;


public class MPNChatMetadataAdapter implements MetadataAdapter {
    
    /**
     * The name of the only item handled by the Chat Data Adapter.
     * Must match the name specified in the Chat Data Adapter source code.
     */
    private static final String ITEM_NAME = "chat_room";

    /**
     * The associated feed to which messages will be forwarded;
     * it is the Data Adapter itself.
     */
    private volatile ChatDataAdapter chatFeed;

    /**
     * The name of the Data Adapter currently bound to this Metadata Adapter,
     * as received through onDataAdapterAvailable.
     */
    private String chatDataAdapterName;

    /**
     * Private logger; a specific "LS_demos_Logger.Chat" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;

    /**
     * Used as a Set, to keep unique identifiers for the currently connected
     * clients.
     * Each client is uniquely identified by the client IP address and the
     * HTTP user agent; in case of conflicts, a custom progressive is appended
     * to the user agent. This set lists the concatenations of the current
     * IP and user agent pairs, to help determining uniqueness.
     */
    private final ConcurrentHashMap<String,Object> uaIpPairs = new ConcurrentHashMap<String,Object>();


    /////////////////////////////////////////////////////////////////////////
    // Initialization

    public MPNChatMetadataAdapter() {}

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        
        /*
        String logConfig = (String) params.get("log_config");
        if (logConfig != null) {
            File logConfigFile = new File(configDir, logConfig);
            String logRefresh = (String) params.get("log_config_refresh_seconds");
            if (logRefresh != null) {
                DOMConfigurator.configureAndWatch(logConfigFile.getAbsolutePath(), Integer.parseInt(logRefresh) * 1000);
            } else {
                DOMConfigurator.configure(logConfigFile.getAbsolutePath());
            }
        }*/
        logger = LogManager.getLogger("LS_demos_Logger.Chat");

        logger.info("ChatMetadataAdapter ready");
    }

    @Override
    public void onDataAdapterAvailable(String dataAdapterName, Object dataAdapter) {
        if (!(dataAdapter instanceof ChatDataAdapter)) {
            return;
        }
        if (chatFeed != null) {
            logger.warn("A second ChatDataAdapter instance is available: " + dataAdapterName + "; ignoring it");
            return;
        }
        chatFeed = (ChatDataAdapter) dataAdapter;
        chatDataAdapterName = dataAdapterName;
        logger.info("ChatDataAdapter bound: " + dataAdapterName);
    }

    @Override
    public void onDataAdapterUnavailable(String dataAdapterName) {
        if (chatDataAdapterName != null && chatDataAdapterName.equals(dataAdapterName)) {
            chatFeed = null;
            chatDataAdapterName = null;
            logger.info("ChatDataAdapter unbound: " + dataAdapterName);
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // Login and session handling

    @Override
    public CompletionStage<SessionRequestHandler> loginAsync(LoginInfo loginInfo) {
        //////////////////////////////////////////////////////////////////////////
        // WARNING: No authentication logic is implemented; any login is accepted.
        // This is suitable for demos only and MUST NOT be used in production.
        //////////////////////////////////////////////////////////////////////////
        return CompletableFuture.completedFuture(new SessionRequestHandler() {

            /**
             * Keeps the client context information supplied by Lightstreamer on the
             * new session notification.
             * Session information is needed to uniquely identify the client.
             */
            private Map<String, String> sessionInfo;

            /**
             * Modifies the clientContext to provide a unique identification
             * for the client session.
             */
            private void uniquelyIdentifyClient(Map<String, String> clientContext) {

                // Extract user agent and ip from session infos
                String ua = clientContext.get("USER_AGENT");
                String ip = clientContext.get("REMOTE_IP");

                /*
                 * we need to ensure that each pair IP-User Agent is unique so that
                 * the sender can be identified on the client, if such pair is not
                 * unique we add a counter on the user agent string
                 */

                String count = "";
                int c = 0;
                
                // Synchronize to ensure that we do not generate two identical pairs
                synchronized(uaIpPairs) {
                    while (uaIpPairs.containsKey(ip+" "+ua+" "+count)) {
                        c++;
                        count = "["+c+"]";
                    }

                    ua = ua+" "+count;

                    uaIpPairs.put(ip+" "+ua,new Object());
                }

                clientContext.put("USER_AGENT", ua);
            }
            
            private void doLog(String msg) {
                if (logger != null) {
                    String IP =  sessionInfo.get("REMOTE_IP");
                    logger.info(msg + "\tfrom " + IP);
                }
            }

            private void loadChatFeed() throws CreditsException {
                if (chatFeed == null) {
                    // The feed is not yet available, maybe the
                    // Chat Data Adapter was not included in the Adapter Set
                    logger.error("ChatDataAdapter not found");
                    throw new CreditsException("No chat feed available", 0, "No chat feed available");
                }
            }

            private void handleChatMessage(String[] pieces, String message) throws MessageException {
                // Extract session infos

                if (pieces.length != 2) {
                    logger.warn("Wrong message received: " + message);
                    throw new MessageException("Wrong message received");
                }

                if (sessionInfo == null) {
                     logger.warn("Message received from non-existent session: " + message);
                     throw new MessageException("Wrong message received");
                }
                
                // Read from infos the IP and the user agent of the user
                String ua= sessionInfo.get("USER_AGENT");
                String ip=  sessionInfo.get("REMOTE_IP");

                // Check the message, it must be of the form "CHAT|message"
                if (pieces[0].equals("CHAT")) {
                    
                    // And send it to the feed
                    if (!chatFeed.sendMessage(ip,ua,pieces[1])) {
                         logger.warn("Wrong message received: " + message);
                         throw new MessageException("Wrong message received");
                    }
                    
                } else {
                     logger.warn("Wrong message received: " + message);
                     throw new MessageException("Wrong message received");
                }
            }

            @Override
            public Double getAllowedMaxBandwidth() {
                return null;
            }

            @Override
            public void onStart(SessionListener listener) {
                // We can't have duplicate sessions
                assert(sessionInfo == null);

                /* If needed, modify the user agent and store it directly
                 * in the session infos object.
                 * Note: we are free to change and store the received object.
                 */
                sessionInfo = new HashMap<>(loginInfo.getClientContext());
                uniquelyIdentifyClient(sessionInfo);
            }

            @Override
            public void onClose() {
                // The session must exist to be closed
                assert(sessionInfo != null);

                // We have to remove session information from the pairs "Set"

                String ua = sessionInfo.get("USER_AGENT");
                String IP =  sessionInfo.get("REMOTE_IP");

                uaIpPairs.remove(IP+" "+ua);
                sessionInfo = null;
            }

            @Override
            public CompletionStage<SubscriptionHandler> newSubscription(SubscriptionInfo info) {
                return CompletableFuture.completedFuture(new SubscriptionHandler(info));
            }

            /**
             * Triggered by a client "sendMessage" call.
             * The message encodes a chat message from the client.
             */
            @Override
            public CompletionStage<String> userMessageAsync(String message)
                throws CreditsException, MessageException {

                // we won't introduce blocking operations, hence we can proceed inline

                if (message == null) {
                    logger.warn("Null message received");
                    throw new MessageException("Null message received");
                }

                // Split the string on the | character
                // The message must be of the form "CHAT|message"
                String[] pieces = message.split("\\|");

                loadChatFeed();
                handleChatMessage(pieces, message);

                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<Void> notifyMpnDeviceAccess(MPNDeviceInfo info)
                throws CreditsException {
                // Authorize all devices
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<MPNActivationHandler> newMPNActivation(MPNActivationInfo info)
                throws CreditsException {

                // We suppose someone may try to communicate with the Server directly, through
                // the TLCP protocol, and force it to send unexpected or unwanted push notifications.
                // They could try to change both the items and the format of legit push notifications,
                // but could not change the app name, as it is checked by the Server during subscription.
                // Hence notifications may only be delivered to the legit app. They could not even change 
                // the trigger expression, as it is filtered by the Server using the regular expression
                // list specified in configuration files.
                
                // Check the item name, should be one of those supported by the StockList Data Adapter 
                List<String> itemNames = info.getItemRefs();
                
                if ((itemNames.size() != 1) ||(!itemNames.get(0).equals(ITEM_NAME)))
                    throw new CreditsException(-102, "Invalid item argument for push notifications");

                return CompletableFuture.completedFuture(new MPNActivationHandler(info) {
                    @Override
                    public CompletionStage<Void> notifyMpnSubscriptionActivation(MPNSubscriptionInfo mpnSubscription)
                        throws CreditsException {

                        // Check the platform type
                        if (mpnSubscription.getDevice().getType() == MPNPlatformType.APPLE) {

                            // Here, we can add APNS-related checks, by inspecting the JSON string returned by:
                            // mpnSubscription.getNotificationFormat()
                            
                            // Authorized, log it
                            doLog("Authorized APNS subscription with parameters:\n" + 
                                    "\titems: " + itemNames + "\n" +
                                    "\tfields: " + info.getFieldRefs() + "\n" +
                                    ((mpnSubscription.getTrigger() != null) ? "\ttrigger expression: " + mpnSubscription.getTrigger() + "\n" : "") +
                                    ((mpnSubscription.getNotificationFormat() != null) ? "\tformat: " + mpnSubscription.getNotificationFormat() + "\n" : ""));
                                    // Here, we can add custom APNS log, by inspecting the JSON string returned by:
                                    // mpnSubscription.getNotificationFormat()
                        
                        } else if (mpnSubscription.getDevice().getType() == MPNPlatformType.GOOGLE) {

                            // Here, we can add FCM-related checks, by inspecting the JSON string returned by:
                            // mpnSubscription.getNotificationFormat()
                            
                            // Authorized, log it
                            doLog("Authorized FCM subscription with parameters:\n" + 
                                    "\titems: " + itemNames + "\n" +
                                    "\tfields: " + info.getFieldRefs() + "\n" +
                                    ((mpnSubscription.getTrigger() != null) ? "\ttrigger expression: " + mpnSubscription.getTrigger() + "\n" : "") +
                                    ((mpnSubscription.getNotificationFormat() != null) ? "\tformat: " + mpnSubscription.getNotificationFormat() + "\n" : ""));
                                    // Here, we can add custom FCM log, by inspecting the JSON string returned by:
                                    // mpnSubscription.getNotificationFormat()
                            
                        } else {
                            throw new CreditsException(-103, "Invalid platform argument for push notifications");
                        }
                        return CompletableFuture.completedFuture(null);
                    }
                });
            }

            @Override
            public CompletionStage<Void> notifyMpnDeviceTokenChange(MPNDeviceInfo info, String newToken)
                throws CreditsException {
                // Authorize all token changes
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    @Override
    public void shutdown() {
    }
}