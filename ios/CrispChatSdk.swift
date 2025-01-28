import Crisp

@objc(CrispChatSdk)
class CrispChatSdk: RCTEventEmitter {
    
    var cancelActions: [CallbackToken] = []
    var currentSessionId: String? = nil

    @objc
    func configure(_ websiteId: String) {
        CrispSDK.configure(websiteID: websiteId)
    }

    @objc
    func setTokenId(_ id: String) {
        CrispSDK.setTokenID(tokenID: id)
    }

    @objc
    func setUserEmail(_ email: String) {
        CrispSDK.user.email = email
    }

    @objc
    func setUserNickname(_ nickname: String) {
        CrispSDK.user.nickname = nickname
    }
    @objc
    func setUserPhone(_ phone: String) {
        CrispSDK.user.phone = phone
    }

    @objc
    func setUserAvatar(_ url: String) {
        CrispSDK.user.avatar = URL(string: url)
    }

    @objc
    func setSessionSegment(_ segment: String) {
        CrispSDK.session.segment = segment
    }

    @objc
    func setSessionString(_ key: String, value: String) {
        CrispSDK.session.setString(value, forKey: key)
    }

    @objc
    func setSessionBool(_ key: String, value: Bool) {
        CrispSDK.session.setString(String(value), forKey: key)
    }

    @objc
    func setSessionInt(_ key: String, value: Int) {
        CrispSDK.session.setInt(value, forKey: key)
    }

    @objc
    func pushSessionEvent(_ eventName: String, color: Crisp.SessionEventColor) {
        CrispSDK.session.pushEvent(Crisp.SessionEvent(name: eventName, color: color))
    }

    @objc
    func resetSession() {
        currentSessionId = nil
        CrispSDK.session.reset()
    }

    @objc
    func show() {
        DispatchQueue.main.async {
            var viewController = RCTPresentedViewController()

            if viewController == nil {
                viewController = UIApplication.shared.windows.first?.rootViewController
            }

            viewController?.present(ChatViewController(), animated: true)
           
            self.addCallbacks()
        }
    }

    func extractUser(from user: Message.Sender) -> String {
        switch user {
            case .`operator`:
                return "operator"
            case .user:
                return "user"
            default:
                return ""
            }
    }
    
    func extractText(from content: Message.Content) -> String {
        switch content {
            case .text(let textValue):
                return textValue
            case .textWithAttachment(let textValue, _, _):
                return textValue
            case .textWithVideoAttachment(let textValue, _, _):
                return textValue
            default:
                return ""
        }
    }
    
    func getJSMessageObject (message:Message) -> [String : Any] {
        return ["content": ["text": self.extractText(from:message.content)], "fingerprint": message.fingerprint, "from": self.extractUser(from:message.from), "user": ["user_id": message.user?.userId, "nickname" : message.user?.nickname]] as [String : Any]
    }
    
    func addCallbacks () {
        let cancelSessionLoaded = CrispSDK.addCallback(.sessionLoaded { sessionId in
            self.currentSessionId = sessionId
            self.sendEvent(withName: "onSessionLoaded", body: ["sessionId": sessionId])
            print("onSessionLoaded", sessionId)
        })
        
        let cancelMessageSent = CrispSDK.addCallback(.messageSent { message in
            self.sendEvent(withName: "onMessageSent", body: ["message": self.getJSMessageObject(message:message)])
            print("onMessageSent", message)
        })
        
        let cancelMessageReceived = CrispSDK.addCallback(.messageReceived { message in
            self.sendEvent(withName: "onMessageReceived", body: ["message": self.getJSMessageObject(message:message)])
            print("onMessageReceived", message)
        })
        
        let cancelChatOpened = CrispSDK.addCallback(.chatOpened {
            self.sendEvent(withName: "onChatOpened", body: ["sessionId": self.currentSessionId])
            print("onChatOpened")
        })
        
        let cancelChatClosed = CrispSDK.addCallback(.chatClosed {
            self.sendEvent(withName: "onChatClosed", body: ["sessionId": self.currentSessionId])
            print("onChatClosed")
            self.removeCallbacks()
        })
        
        cancelActions.append(cancelSessionLoaded)
        cancelActions.append(cancelMessageSent)
        cancelActions.append(cancelMessageReceived)
        cancelActions.append(cancelChatOpened)
        cancelActions.append(cancelChatClosed)
    }
    
    func removeCallbacks () {
        for action in cancelActions {
            CrispSDK.removeCallback(token:action)
        }
    }

    override func supportedEvents() -> [String]! {
        return ["onSessionLoaded","onChatOpened", "onChatClosed", "onMessageSent", "onMessageReceived"]
    }
    
    @objc
    func searchHelpdesk () {
        CrispSDK.searchHelpdesk()
    }

    @objc
    func openHelpdeskArticle (_ id:String, locale:String, title: String? = "", category: String? = "") {
        CrispSDK.openHelpdeskArticle(locale: locale, slug: id, title:title, category: category)
    }
    
    @objc
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }


}
