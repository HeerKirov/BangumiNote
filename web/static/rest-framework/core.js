/**与REST风格的API进行对接的核心工具集。
 */
var restful = {
    /** 构造用于LIST请求的核心组件。
     * info: {url: String, method: String = 'GET'}
     */
    newlist: function (info) {
        var _info = info;
        var _hiddenparams = null;
        var geturl = function (url, params) {
            var ret;
            if(url.substring(url.length-5)===".json")ret = url;
            else ret = url + ".json";

            var p_std = param_to_str(params);
            var p_hidden = param_to_str(_hiddenparams);
            if(p_std!==null&&p_hidden!==null){
                ret += "?" + p_std+ "&" + p_hidden;
            }else if(p_std!==null){
                ret += "?" + p_std;
            }else if(p_hidden!==null){
                ret += "?" + p_hidden;
            }
            return ret;
        };
        var param_to_str = function (params) {

            if(params!==null){
                var ret = "";
                var first = true;
                for(var i in params) {
                    if(first)first = false;else ret += "&";
                    if($.isArray(params[i])) {
                        var second = true;
                        for(var j in params[i]) {
                            if(second)second = false;else ret += "&";
                            ret += i + "=" + encodeURIComponent(params[i][j]);
                        }
                    }else{
                        ret += i + "=" + encodeURIComponent(params[i]);
                    }
                }
                return ret;
            }else return null;
        };
        var obj = {
            /**启动一次request请求。
             * requestinfo: {param...} 附加给本次请求的额外参数。
             * deleagte: function(bool, int, json) 完成本次请求之后，回调此函数，依次传入[是否成功/HTTP状态码/内容JSON]。
             */
            request: function (requestinfo, delegate) {
                var url = geturl(_info.url, requestinfo);
                $.ajax({
                    type: _info.method,
                    async: true,
                    url: url,
                    dataType: "json",
                    success: function(data, textStatus, xhr) {
                        delegate(true, xhr.status, data);
                    },
                    error: function (xhr, textStatus, errorThown) {
                        var json;try{json = JSON.parse(xhr.responseText);}catch(e){json = {};}
                        if(textStatus === "timeout")delegate(false, 408, json);
                        else if(textStatus === "abort")delegate(false, 500, json);
                        else if(textStatus === "parsererror")delegate(false, 400, json);
                        else delegate(false, xhr.status, json);
                    }
                });
            },
            appendparams: function (params) {
                _hiddenparams = params;
                return this;
            }
        };
        if(!("method" in _info))_info["method"] = "GET";
        return obj;
    },
    /** 用于构造CREATE请求的核心组件。
     * info: {url: String, method: String = 'POST', proxy: bool = true}
     *              proxy: 使用json form代理发送。这将把json转换为string，并附加在[json]表单项目中发送。
     */
    newcreate: function (info) {
        var _info = info;
        var geturl = function (url) {
            var ret;
            if(url.substring(url.length-5)===".json")ret = url;
            else ret = url + ".json";
            return ret;
        };
        var obj = {
            /**启动一次request请求。
             * requestinfo: json 附加给本次请求的内容。
             * deleagte: function(bool, int, json) 完成本次请求之后，回调此函数，依次传入[是否成功/HTTP状态码/内容JSON]。
             */
            request: function (requestinfo, delegate) {
                var url = geturl(_info.url);
                var data = _info.proxy? {json: JSON.stringify(requestinfo)}: requestinfo;
                $.ajax({
                    type: _info.method,
                    async: true,
                    url: url,
                    dataType: "json",
                    data: data,
                    success: function (data, textStatus, xhr) {
                        delegate(true, xhr.status, data);
                    },
                    error: function (xhr, textStatus, errorThown) {
                        var json;try{json = JSON.parse(xhr.responseText);}catch(e){json = {};}
                        if(textStatus === "timeout")delegate(false, 408, json);
                        else if(textStatus === "abort")delegate(false, 500, json);
                        else if(textStatus === "parsererror")delegate(false, 400, json);
                        else delegate(false, xhr.status, json);
                    }
                });
            }
        };
        if(!("method" in _info))_info["method"] = "POST";
        if(!("proxy" in _info))_info["proxy"] = true;
        return obj;
    },
    /** 构造用于detail系列请求的核心组件。可以针对单个detail发送retrieve update delete请求。
     * info {
     *      url: string
     *      updateUrl: string = url 额外指定update用的url。不指定时都用url。
     *      deleteUrl: string = url 额外指定delete用的url。不指定时都用url。
     *      retrieveMethod: string = "GET"
     *      updateMethod: string = "PUT"
     *      partialUpdateMethod: string = "PATCH
     *      deleteMethod: string = "DELETE"
     *      proxy: bool = true
     * }
     */
    newdetail: function (info) {
        var _info = info;
        var geturl = function (url) {
            var ret;
            if(url.substring(url.length-5)===".json")ret = url;
            else ret = url + ".json";
            return ret;
        };
        var obj = {
            "retrieve": function (delegate) {
                var url = geturl(_info.url);
                $.ajax({
                    type: _info.retrieveMethod,
                    async: true,
                    url: url,
                    dataType: "json",
                    success: function (data, textStatus, xhr) {
                        delegate(true, xhr.status, data);
                    },
                    error: function (xhr, textStatus, errorThown) {
                        var json;try{json = JSON.parse(xhr.responseText);}catch(e){json = {};}
                        if(textStatus === "timeout")delegate(false, 408, json);
                        else if(textStatus === "abort")delegate(false, 500, json);
                        else if(textStatus === "parsererror")delegate(false, 400, json);
                        else delegate(false, xhr.status, json);
                    }
                });
            },
            "update": function (requestInfo, partial, delegate) {
                var url = geturl(_info.updateUrl);
                var data = _info.proxy? {json: JSON.stringify(requestInfo)}: JSON.stringify(requestInfo);
                $.ajax({
                    type: partial? _info.partialUpdateMethod : _info.updateMethod,
                    async: true,
                    url: url,
                    dataType: "json",
                    data: data,
                    success: function (data, textStatus, xhr) {
                        delegate(true, xhr.status, data);
                    },
                    error: function (xhr, textStatus, errorThown) {
                        var json;try{json = JSON.parse(xhr.responseText);}catch(e){json = {};}
                        if(textStatus === "timeout")delegate(false, 408, json);
                        else if(textStatus === "abort")delegate(false, 500, json);
                        else if(textStatus === "parsererror")delegate(false, 400, json);
                        else delegate(false, xhr.status, json);
                    }
                });
            },
            "delete": function (delegate) {
                var url = geturl(_info.deleteUrl);
                $.ajax({
                    type: _info.deleteMethod,
                    async: true,
                    url: url,
                    dataType: "json",
                    success: function (data, textStatus, xhr) {
                        delegate(true, xhr.status, data);
                    },
                    error: function (xhr, textStatus, errorThown) {
                        var json;try{json = JSON.parse(xhr.responseText);}catch(e){json = {};}
                        if(textStatus === "timeout")delegate(false, 408, json);
                        else if(textStatus === "abort")delegate(false, 500, json);
                        else if(textStatus === "parsererror")delegate(false, 400, json);
                        else delegate(false, xhr.status, json);
                    }
                });
            }
        };
        if(!("updateUrl" in _info))_info["updateUrl"] = _info.url;
        if(!("deleteUrl" in _info))_info["deleteUrl"] = _info.url;
        if(!("retrieveMethod" in _info))_info["retrieveMethod"] = "GET";
        if(!("updateMethod" in _info))_info["updateMethod"] = "PUT";
        if(!("partialUpdateMethod" in _info))_info["partialUpdateMethod"] = "PATCH";
        if(!("deleteMethod" in _info))_info["deleteMethod"] = "DELETE";
        if(!("proxy" in _info))_info["proxy"] = false;
        return obj;
    }
};

var http_status_code_map = {
    400: "Bad Request",
    401: "Unauthorized",
    403: "Forbidden",
    404: "Not Found",
    405: "Method Not Allowed",
    406: "Not Acceptable",
    408: "Request Timeout",
    500: "Internal Server Error",
    502: "Bad Gateway",
    504: "Gateway Timeout"
};
var default_error_status_map = {
    403: "403 由于权限不够，访问被拒绝。",
    404: "404 没有找到请求的资源。",
    405: "405 请求资源的方法不正确。",
    408: "408 请求超时。",
    500: "500 内部服务器发生错误。"
};
var default_error_keyword_map = {
    USER_EXISTS: "该用户已存在。",
    NO_ENOUGH_INFORMATION: "没有提供足够的信息。",
    INFORMATION_FORMAT_WRONG: "发送的信息格式错误。",

    REGISTER_FORBIDDEN: "注册行为已被管理员禁用。",
    AUTHENTICATED_FAILED: "由于权限不够，访问被拒绝。",
    NOT_FOUND: "没有找到资源。",
    METHOD_NOT_ALLOWED: "请求资源的方法不正确。"
};
