
function analysisNotice(info) {
    var noticeButton = info["noticeButton"];
    var noticeButtonSmall = info["noticeButtonSmall"];
    var messageRequest = info["messageRequest"];
    var noticeModal = info["noticeModal"];
    var body = info["noticeModalBody"];
    var newMessageExist = false;
    var haveMessage = false;
    var setMessageButton = function() {
        if(newMessageExist) {
            noticeButton.find("i").attr("class", "fa fa-commenting");
            noticeButtonSmall.html('消息 <i class="fa fa-info"></i>');
        }else{
            noticeButton.find("i").attr("class", "fa fa-comment-o");
            noticeButtonSmall.html('消息');
        }
    };
    messageRequest.request("exists", null, function(success, status, data) {
        if(success) {
            newMessageExist = (data["exist"]!==undefined)?data["exist"]:false;
            setMessageButton();
        }
    });
    var requestMessageList = function() {
        if(newMessageExist) {
            newMessageExist = false;
            setMessageButton();
        }
        noticeModal.modal('show');
        if(!haveMessage) {//只读取一遍消息。
            haveMessage = true;
            body.html('<i class="fa fa-circle-o-notch fa-spin fa-3x"></i>');
            messageRequest.request("unread", null, function(success, status, data) {
                body.html("");
                if(success) {
                    var content = data["content"];
                    if(content.length === 0)body.html('<label class="text-center"><small>没有新的消息<small></label>');
                    else for(var i = 0; i < content.length; ++i) {
                        var item = content[i];
                        var v = "";
                        if(item["type"] === "general") {
                            v = '<strong>' + item["content"]["title"] + ' </strong><small>' + item["content"]["content"] + '</small>';
                        }else if(item["type"] === "diary_publish") {
                            v = '<strong>' + item["content"]["name"] + ' </strong><small>第';
                            var first = true;
                            for(var index = item["content"]["old_count"] + 1; index <= item["content"]["new_count"]; ++index) {
                                if(first){first = false;}else{v += ', '}
                                v += index;
                            }
                            v += '话 已经更新</small>';
                        }
                        var notice = $('<div class="card m-2"></div>')
                            .append($('<div class="card-body"></div>')
                                .html(v));
                        body.append(notice)
                    }
                }else{
                    body.html('<label class="text-center"><small>没有新的消息<small></label>');
                }
            });
        }
    };
    noticeButton.click(requestMessageList);
    noticeButtonSmall.click(requestMessageList);
}