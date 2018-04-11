/**用于创建对接REST风格的api的create功能的工具集。
 * 功能包括：
 * 1. 生成具有自定义特性的新建表单
 * 2. 向自定义的委托进行CREATE调用
 */
/** 构造create面板。
 * obj: 建议使用一个".col"的div面板。
 * info: {
 *      title: string = null 表头
 *
 *      width: int = 12 宽度格数。会被自动计算填充。
 *
 *      successUrl: string|function(json) = "#" 成功提交后的跳转。可以给出一个函数来自定义跳转，函数的参数为成功提交后返回的content。
 *
 *      validate: function(json) = null 总的合法性检查函数。可以检查成型的json结构并返回修正后的结构。可以抛出异常以提醒检查器。
 *                                           抛出的异常如果是字符串，会直接界定为总错误;如果是[{index, msg}]结构，就会将其分摊到不同的元素上。
 *      content: [ 行的配置列表。
 *          {
 *              header: string 行名。
 *              field: string 在json中的字段名称。
 *              type: string|function(string) = null 数据类型。可以不写然后按默认处理。也可以传入一个自定义函数。
 *                                                      如果你给出了一个自定义类型函数，你必须给出自定义validate函数，并使用此函数提取内容值。
 *              typeInfo: object = null 附加给类型的额外内容。
 *              defaultValue: any = null 默认值。
 *              validate: function(value) = null 针对该field的合法性检查函数，检查提取之后成型之前的value，返回实际value。可以抛出异常提醒检查器。
 *          }
 *      ],
 *      errorStatus: {
 *          <HTTP_STATUS: MESSAGE>
 *      },
 *      errorKeyword: {
 *          <KEYWORD: MESSAGE>
 *      }
 * }
 * data: function(json) 简单的数据输出位。只需要提供一个简单接受json内容的函数。
 * rest: function(json, function(bool, int, json)) 对接REST结构的数据输出位。应当额外提供一个回调参数，该函数传入success|status|content.
 */
/** html结构：
 * - obj.col 被传入的顶级结构
 *      - title_div.row
 *          |title_div.col|function_button_div.col|
 *              - h2            - div.btn-group
 *      - panel_div.row
 *          |table_div.col|
 *              - ...
 *      - error_message.row
 *          |error_message_div.col|
 *              - ...
 *       - bottom_div.row
 *          |bottom_div.col|
 *              - ...
 */
function build_create(obj) {
    var _info = null;
    var _setter = null;
    var _restsetter = null;

    var _running = false; //标记是否有任务正在进行中。

    var _obj = obj; //构造面板的jquery object。
    var _submit_btn = null; //提交按钮。
    var _eles = []; //所有输入元素的列表。
    var _ele_error_divs = []; //所有输入元素所在的表格列的列表。记录这个列表用于构造错误提示框。
    var _sum_error_div = null; //用于放置总错误栏的列。

    var build_title_div = function () {
        //构造title部分。
        return $('<div class="row mb-4"></div>')
            .append($('<div class="col"></div>').append($('<h2></h2>').text(_info.title)))
            .append($('<div class="col-auto"></div>').append(build_function_buttons()));
    };
    var build_function_buttons = function () {
        var group = $('<div class="btn-group"></div>');
        //TODO 功能按钮组
        return group;
    };
    var build_panel_div = function () {
        //构造主面板。
        var _panel_div_col = $('<div class="col"></div>');
        _panel_div_col.append(build_table());
        return $('<div class="row mt-5"></div>').append(_panel_div_col);
    };
    var build_table = function () {
        var tbody = $('<tbody></tbody>');
        for(var i in _info.content){
            var field = _info.content[i];
            var header = $('<label></label>').text(field.header);
            var tdcontent = build_create_field(field);
            _eles[i] = tdcontent;
            var err = $('<div class="col"></div>');
            _ele_error_divs[i] = err;
            tbody
                .append($('<tr></tr>')
                    .append($('<td></td>')
                        .append(header))
                    .append($('<td></td>')
                        .append(tdcontent)
                        .append($('<div class="row"></div>')
                            .append(err)))
            );
        }
        return $('<table class="table"></table>').append(tbody);
    };
    var build_error_bottom_div = function () {
        //构造底栏上关于错误提示框的一行。
        var div = $('<div class="col"></div>');
        _sum_error_div = div;
        return $('<div class="row"></div>').append(div);
    };
    var build_bottom_div = function () {
        //构造底部面板。
        var create_btn = $('<button class="btn btn-secondary btn-block mt-4">创建</button>');
        create_btn.click(do_submit);
        _submit_btn = create_btn;
        var _bottom_div_col = $('<div class="col"></div>').append(create_btn);
        return $('<div class="row"></div>')
            .append($('<div class="col"></div>'))
            .append(_bottom_div_col)
            .append($('<div class="col"></div>'));
    };
    
    var change_submit_status = function (loading) {
        //修改提交按钮的可用状态。这会在加载态和可用态之间切换。
        if(loading){
            _submit_btn.html("");
            _submit_btn.attr("class", "btn disabled btn-block");
            _submit_btn.append($('<i class="fa fa-circle-o-notch fa-spin fa-fw"></i>'));
        }else{
            _submit_btn.html("");
            _submit_btn.attr("class", "btn btn-secondary btn-block");
            _submit_btn.text("创建");
        }
    };

    var do_submit = function () {
        if(!_running)_running = true;
        else return;
        //附加给提交按钮的提交行为。
        //首先，需要做冗长的内容提取、检查、合并任务。
        clear_err_message(); //在提交之前先清除上次留下的错误信息。
        change_submit_status(true);
        var result = do_collect_and_check();
        if(result!==undefined) {
            //内容检查完成，可以向setter提交。
            if(_restsetter!==null&&$.isFunction(_restsetter)){
                _restsetter(result, function (success, status, json) {
                    if(success){
                        if($.isFunction(_info.successUrl)){
                            location.href = _info.successUrl(json)
                        }else{
                            location.href = _info.successUrl;
                        }
                    }else{
                        change_submit_status(false);
                        show_err_request(status, json);
                    }
                    _running = false;
                });
            }else if(_setter!==null&&$.isFunction(_setter)){
                _setter(result);
                if($.isFunction(_info.successUrl)){
                    location.href = _info.successUrl(null)
                }else{
                    location.href = _info.successUrl;
                }
                change_submit_status(false);
                _running = false;
            }else{
                change_submit_status(false);
                _running = false;
            }
        }else{
            change_submit_status(false);
            _running = false;
        }
    };
    var do_collect_and_check = function() {
        //从控件收集内容并检查。
        //成功时返回json;失败时，返回undefined。
        var err_happend = false;
        var ele_lists = {};
        var json = {};
        for(var i in _info.content) {
            var field = _info.content[i];
            var ele = _eles[i];
            try {
                json[field.field] = check_create_field(field, ele);
            }catch(e){
                ele_lists[i] = e;
                err_happend = true;
            }
        }
        if(err_happend){
            for(i in ele_lists){
                if(ele_lists[i] !== undefined) show_err_message(i, ele_lists[i]);
            }
            return undefined;
        }
        if(_info.validate !== null) {
            try {
                json = _info.validate(json);
            }catch(e){
                if(e instanceof Array) {
                    for(i in e) {
                        if(e[i] instanceof Object) {
                            var index = e[i]["index"];
                            var msg = e[i]["msg"];
                            if(index) {
                                show_err_message(index, msg);
                            }
                        }else{
                            show_err_message(-1, e[i]);
                        }
                    }
                }else{
                    show_err_message(-1, e);
                }
                return undefined;
            }
        }
        return json;
    };
    var show_err_message = function (index, message) {
        //打开错误展示。
        //index表示打开错误展示的field，若为负数表示展示在总错误栏内。
        var err = $('<div class="alert alert-danger mt-2 alert-dismissable"></div>')
            .append($('<button type="button" class="close btn-sm" data-dismiss="alert">&times;</button>'))
            .append(message);
        if(index < 0) {
            _sum_error_div.append(err);
        }else if(index < _ele_error_divs.length){
            _ele_error_divs[index].append(err);
        }
    };
    var show_err_request = function (status, json) {
        //打开复杂的错误展示。这将从相关结构中提取复杂的错误信息。
        var message;
        if(json!==null&&("keyword" in json)&&(json.keyword in _info.errorKeyword)) {
            message = _info.errorKeyword[json.keyword];
        }else if(status in _info.errorStatus){
            message = _info.errorStatus[status];
        }else if(json!==null&&("message" in json)){
            message = json.message;
        }else if(status in http_status_code_map) {
            message = http_status_code_map[status];
        }else{
            message = status;
        }
        show_err_message(-1, message);
    };
    var clear_err_message = function () {
        //清除所有的错误展示。
        for(var i in _ele_error_divs) _ele_error_divs[i].html("");
        _sum_error_div.html("");
    };

    return {
        info: function (info) {
            _info = info;
            //填充info的默认值。
            if(!("title" in _info))_info["title"] = null;
            if(!("width" in _info))_info["width"] = null;

            if(!("successUrl" in _info))_info["successUrl"] = "#";

            if(!("validate" in _info))_info["validate"] = null;
            if(!("content" in _info))_info["content"] = [];
            //追加错误处理数据。
            if(!("errorStatus" in _info))_info["errorStatus"] = {};
            if(!("errorKeyword" in _info))_info["errorKeyword"] = {};
            for(var k in default_error_status_map)
                if(!(k in _info.errorStatus))_info.errorStatus[k] = default_error_status_map[k];
            for(k in default_error_keyword_map)
                if(!(k in _info.errorKeyword))_info.errorKeyword[k] = default_error_keyword_map[k];
            //处理每一个field的内容默认值。
            for(var i in _info.content) {
                var field = _info.content[i];
                if(!("type" in field))field["type"] = null;
                if(!("typeInfo" in field))field["typeInfo"] = {};
                if(!("defaultValue" in field))field["defaultValue"] = null;
                if(!("validate" in field))field["validate"] = null;

                initialization_create_field_info(field);
            }
            _obj.append(build_title_div()).append(build_panel_div()).append(build_error_bottom_div()).append(build_bottom_div());
            return this;
        },
        data: function (data_setter) {
            _setter = data_setter;
            return this;
        },
        rest: function (restful_setter) {
            _restsetter = restful_setter;
            return this;
        }
    };
}

function initialization_create_field_info(field) {
    var t;if(field.type === null) t = create_field_initializations[default_create_field_elements];
    else if(field.type in create_field_initializations) t = create_field_initializations[field.type];
    if(t&&$.isFunction(t)) t(field.typeInfo)
}

function build_create_field(field) {
    //按照field.type | field.typeInfo | field.default 来构建内容。
    var ele;
    if(field.type !== null && $.isFunction(field.type)){
        ele = field.type(dataitem);
    }else if(field.type === null){
        ele = create_field_elements[default_create_field_elements](field.typeInfo, field.defaultValue);
    }else if(field.type in create_field_elements){
        ele = create_field_elements[field.type](field.typeInfo, field.defaultValue);
    }else{
        ele = null;
    }
    return ele;
}

function check_create_field(field, ele) {
    //提取元素内容，并做类型检查和field级自定义检查。
    //此层不捕捉异常。
    var value;
    if(field.type !== null && $.isFunction(field.type)){
        value = ele;
    }else if(field.type === null){
        value = create_field_validates[default_create_field_elements](field.typeInfo, ele);
    }else if(field.type in create_field_validates){
        value = create_field_validates[field.type](field.typeInfo, ele);
    }else{
        value = null;
    }
    if(value !== null && field.validate !== null) value = field.validate(value);
    return value;
}

var create_field_initializations = {
    text: function (info) {
        if(!("allowBlank" in info))info["allowBlank"] = true;
        if(!("allowNull" in info))info["allowNull"] = false;
    },
    password: function (info) {
        if(!("allowBlank" in info))info["allowBlank"] = true;
        if(!("allowNull" in info))info["allowNull"] = false;
    },
    number: function (info) {
        if(!("allowBlank" in info))info["allowBlank"] = true;
        if(!("allowNull" in info))info["allowNull"] = false;
        if(!("allowFloat" in info))info["allowFloat"] = true;
    },
    datetime: function (info) {
        if(!("allowBlank" in info))info["allowBlank"] = true;
        if(!("allowNull" in info))info["allowNull"] = false;
        if(!("format" in info))info["format"] = "yyyy-mm-dd hh:ii:ss";
        if(!("view" in info))info["view"] = "minute";
        if(!("defaultView" in info))info["defaultView"] = "month";
    }
};
/** create的field的构建模式。
 * <typeName> : function(typeInfo: any, default: any = null) 给出该构建模式的构建函数。
 *                          传入的参数为【类型参数】、【默认值】。默认值为null时表示没有默认值。
 *                          也可以不理会默认值。
 */
var create_field_elements = {
    text: function(info, default_value) {
        var ret = $('<input type="text" class="form-control"/>');
        if(default_value!==null)ret.val(default_value);
        if(info){
            if(("placeholder" in info))ret.attr("placeholder", info["placeholder"]);
        }
        return ret;
    },
    password: function(info, default_value) {
        var ret = $('<input type="password" class="form-control"/>');
        if(default_value!==null)ret.val(default_value);
        if(info){
            if(("placeholder" in info))ret.attr("placeholder", info["placeholder"]);
        }
        return ret
    },
    number: function(info, default_value) {
        var ret = $('<input type="text" class="form-control"/>');
        if(default_value!==null)ret.val(default_value);
        if(info){
            if(("placeholder" in info))ret.attr("placeholder", info["placeholder"]);
        }
        return ret;
    },
    datetime: function (info, default_value) { //默认值应当是date类型的。
        var format = "yyyy-mm-dd hh:ii:ss";
        var view = 0;
        var defaultView = 2;
        if(info){
            if("format" in info) format = info.format;
            if("view" in info) view = datetimepicker_range_to_num(info.view);
            if("defaultView" in info) defaultView = datetimepicker_range_to_num(info.defaultView);
        }
        var com_default_value = default_value!==null?fmt_dt_date(default_value, format):"";//将默认值转换为可读的标准格式。
        var ret = $('<div class="btn-group input-append date form_datetime"></div>')
            .append($('<input class="form-control rounded-0" size="16" type="text" readonly>').val(com_default_value))
            .append($('<button class="btn btn-secondary add-on"></button>')
                .append($('<i class="fa fa-calendar"></i>')));
        ret.datetimepicker({format: format, todayBtn: true, pickerPosition: "bottom-left", minView: view, startView: defaultView});
        ret.datetimepicker('update');
        if(default_value!==null)info["hidden_now_value"] = fmt_dt_date(default_value, "yyyy-m-d h:i:s");
        ret.on('changeDate', function (e) {
            info["hidden_now_value"] = fmt_dt_date(e.date, "yyyy-m-d h:i:s");
        });
        return ret;
    }
};
/** create的field的内容检查。
 * <typeName> : function(typeInfo: any, ele: jQuery) 给出该构建模式的内容提取与检查函数。
 *                      传入的参数为【类型参数】、【jQuery对象】。
 *                      一般来说，应当提取元素的值，并返回该值。
 *                      同时，也应当进行合法性检查。如果检查失败，应当抛出一个异常来提醒检查器。
 */
var create_field_validates = {
    text: function(info, ele) {
        var ret = ele.val().trim();
        if(info){
            if(info.length&&ret.length > info.length)throw "长度不能超过" + info.length + "位。";
            if((!info.allowBlank)&&ret.length === 0)throw "内容不能为空。";
            if(info.allowNull&&ret.length === 0)ret = null;
        }
        return ret;
    },
    password: function (info, ele) {
        var ret = ele.val().trim();
        if(info){
            if(info.length&&ret.length > info.length)throw "长度不能超过" + info.length + "位。";
            if((!info.allowBlank)&&ret.length === 0)throw "内容不能为空。";
            if(info.allowNull&&ret.length === 0)ret = null;
        }
        return ret;
    },
    number: function(info, ele) {
        var ret = ele.val();
        if(info){
            if((!info.allowBlank)&&ret.length === 0)throw "内容不能为空。";
            if(info.allowNull&&ret.length === 0)return null; //在这里直接抛出了。
        }
        var num = parseFloat(ret);
        if(isNaN(num))throw "内容不是合法的数字。";
        if(info){
            if(info.allowFloat===false&&(!Number.isInteger(num))) throw "数值必须为整数。";
            if(isFinite(info.min)&&num<info.min) throw "数值低于允许的最小值" + info.min + "。";
            if(isFinite(info.max)&&num>info.max) throw "数值高于允许的最大值" + info.max + "。";
        }
        return num;
    },
    datetime: function (info, ele) {
        var ret = info["hidden_now_value"];
        if(info){
            if((!info.allowBlank)&&ret===undefined)throw "内容不能为空。";
            if(info.allowNull&&(ret===undefined||ret===null))ret = null;
        }
        return ret;
    }
};
var default_create_field_elements = "text";

/** typeInfo文档：
 *  text: { //default: string
 *      placeholder: string = undefined 为文本框添加底部提示文本
 *
 *      allowBlank: bool = true 是否允许内容为空
 *      allowNull: bool = false 是否允许为null。这将在内容为空时返回null。
 *      length: int = undefined 给出长度限制
 *  }
 *  password: { //default: string
 *      placeholder: string = undefined 为文本框添加底部提示文本
 *
 *      allowBlank: bool = true 是否允许内容为空
 *      allowNull: bool = false 是否允许为null。这将在内容为空时返回null。
 *      length: int = undefined 给出长度限制
 *  }
 *  number: { //default: number
 *      placeholder: string = undefined
 *
 *      allowBlank: bool = true 是否允许空。
 *      allowNull: bool = false 是否允许为null。这将在内容为空时返回null。
 *      min: number = undefined 最小值。
 *      max: number = undefined 最大值。
 *      allowFloat: bool = true 允许浮点数。
 *  }
 *  datetime: { //default: Date
 *      allowBlank: bool = true 是否允许为空。
 *      allowNull: bool = false 是否允许为null。这将在内容为空时返回null。
 *      format: string = "yyyy-mm-dd hh:ii:ss" 默认的日期展示格式。
 *      view: string = 'minute' 可以展示的时间范围。可选[minute|hour|day|month|year]。
 *      defaultView: string = 'month' 默认展示的时间范围。
 *  }
 */
/**TODO create页
 需要做一个匹配List<*>的东西，能够将一个序列的东西转化为列表。内容产生倒不必太严格。
 需要做一个匹配上述模型引用的东西，能够从一个预加载的列表中选择一个已有的东西加入，或者创建一个新的（这用到了两步create）。
 为了优化体验，创建即时生效，刷新预加载列表，并直接加入已选择列表。
 为了泛用性，应当支持many模式。
 为了扩充功能，需要加入一个组件对应多个json field的功能。使field能传入数组就可以。
 */