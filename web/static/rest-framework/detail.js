/** 用于创建对接REST风格的detail详情页的工具集。
 * 功能包括：
 * 1. 构建具有自定义特性的详情页面
 * 2. 从自定义数据源获取数据并填充页面
 * 3. 将详情页部分或全部切换为修改模式，并向自定义修改器提交更改。
 * 4. 向自定义修改器提交删除功能
 * 5. 添加自定义功能工具条
 */
/**构造detail面板。可以将面板输出到指定的jquery对象。
 * obj: 建议使用一個div.col面板。
 * info: {
 *      title: string = null 页表头
 *
 *      defaultPanel: string = "loading" 默认的面板状态。
 *      defaultState: string = "show" 所有的行默认处于何种状态。可取[show|edit]。
 *
 *      validate: function(json) = null 总的合法性检查函数。只会在整体提交时生效。
 *      content: [ 行的配置列表。
 *          {
 *              header: string 行名。
 *              field: string 在json中的字段名称。
 *              type: string = null 数据类型，双向生效。因为内容过多，不允许使用自定义函数构建。
 *              typeInfo: object = null 附加给类型的额外内容。
 *              validate: function(value) = null 针对该field进行提交时的合法性检查函数，检查并返回正确的value。
 *
 *              readable: bool = true 可读。如果不可读，该控件将在展示模式下被隐藏，且只能在整体修改下出现。
 *              writable: bool = true 可写。如果不可写，该控件不会切换到修改模式，也不会参与update响应。
 *              partialWrite: bool = true 可独立修改。如果为否，该控件不能进行单独修改，必须切换为整体修改。
 *
 *              defaultValue: any = null 默认值。在数据加载之前，就预先填入默认值。
 *
 *              defaultState: string = null 该行默认处于何种状态。可取[show|edit]。不能partialWrite的行不能取为edit。
 *          }
 *      ]
 *      updateUrl: string|function(json) = null 进行一次all edit成功之后，进行跳转。
 *      deleteUrl: string = null 删除操作成功之后，跳转到哪里。
 *
 *      allowAllUpdate: bool = true 允许全局编辑。
 *      allowPartialUpdate: bool = true 允许局部编辑。
 *      allowDelete: bool = true 允许删除。
 *
 *      errorStatus: {
 *          <HTTP_STATUS: MESSAGE>
 *      }
 *      errorKeyword: {
 *          <KEYWORD: MESSAGE>
 *      }
 * }
 */
/** html结构：
 * - obj.col
 *      - title_div.row
 *          |title_div.col|function_button_div.col|
 *              - h2             - div.btn-group
 *      - panel_div.row
 *          |table_div.col|
 *              - ...
 *      - loading_div.row
 *          |loading_div.col|
 *              - div.progress
 *      - error_div.row
 *          |error_div.col|
 *              - div.alert
 *      - bottom_error_div.row
 *          |bottom_error_div.col|
 *              - ...
 *      - bottom_div.row
 *          |bottom_div.col|
 *              - ...
 */
function build_detail(obj) {
    var _info = null; //配置信息。
    var _data_get = null;
    var _data_set = null;
    var _data_del = null;
    var _rest_com = null;

    //面板的状态信息 ==================================================================
    var _panel_state = null; //面板的切换状态。{loading|panel|error}
    var _all_field_state = null; //元素的总体状态。 {show|all-edit}
    var _table_state = []; //包含table内元素行的状态。

    var _running = false; //是否有任务正在进行。

    //面板被记录的jquery对象 ==========================================================
    var _obj = obj; //div

    var _func_btn_set = {}; //功能按钮组集合。{div, all_edit_btn, all_submit_btn, delete_btn}
    var _panel_div = null; //panel面板的div
    var _loading_div = null; //loading面板div
    var _error_div = null; //error面板div
    var _bottom_div = null; //bottom面板的div

    var _delete_modal = null; //删除提示面板。

    var _alert_div = null; //error中的alert提示框。
    var _table_set = []; //包含table相关所有元素的列表。内容为：{tr, read_ele, write_ele, error_div, update_btn, submit_btn}

    var _sum_error_div = null; //底栏上的总的错误框。

    //获取相关功能的回调函数 ==========================================================
    var get_func_edit_btn = function (index) {
        //返回一个函数，用在index下标的编辑按钮上。
        return function () {
            //在此ele允许编辑时，将其切换到edit状态。
            if(_info.content[index].writable)set_field_state(index, "edit");
        }
    };
    var get_func_submit_btn = function (index) {
        //返回一个函数，用在index下标的提交按钮上。
        return function () {
            //在事务列表空闲时才能执行。提交一个更改事务并切换到loading状态。
            submit_part_update(index);
        }
    };
    var get_func_submit_all_btn = function () {
        //返回一个函数，用在提交全部的提交按钮上。
        return function () {
            submit_all_update();
        }
    };
    var get_func_switch_edit = function () {
        //返回一个函数，会切换到all edit状态。
        return function () {
            if(isfree()) {
                if(_all_field_state === "show")set_all_field_state("all-edit");
                else set_all_field_state("show");
            }
        }
    };
    var get_func_delete = function () {
        //返回一个函数，触发删除操作。
        return function () {
            submit_delete();
        }
    };

    //进行提交工作的相关函数 ==========================================================
    var submit_part_update = function (index) {
        //提交下标为index的field的值，发送一个patch。
        if(isfree()){
            run();
            var field = _info.content[index];
            clear_err_message(index);
            set_field_state(index, "loading");
            var result = collect_and_check(index);
            if(result!==undefined) {
                var result_json = {};
                result_json[field.field] = result;
                if(_rest_com!==null&&$.isFunction(_rest_com["update"])) {
                    _rest_com["update"](result_json, true, function (success, status, json) {
                        if(success){
                            //提交成功之后会修改展示的值。
                            if(field.field in json)set_value(index, json[field.field]);
                            set_field_state(index, "show");
                        }else{
                            clear_all_err_message();
                            show_err_request(status, json);
                            set_field_state(index, "edit");
                        }
                        free();
                    })
                }else if(_data_set!==null&&$.isFunction(_data_set)) {
                    _data_set(result_json);
                    set_value(index, result);
                    set_field_state(index, "show");
                    free();
                }else{throw "No useful delegate function or component."}
            }else{
                set_field_state(index, "edit");
                free();
            }
        }
    };
    var submit_all_update = function () {
        //提交全部值，发送一个put。
        if(isfree()){
            run();
            clear_all_err_message();
            var result = collect_all_and_check();
            if(result!==undefined) {
                if(_rest_com!==null&&$.isFunction(_rest_com["update"])) {
                    _rest_com["update"](result, false, function (success, status, json) {
                        if(success){
                            //提交成功之后会修改展示的值。
                            set_all_value(json);
                            //并尝试进行网页跳转。
                            if(_info.updateUrl!==null){
                                if($.isFunction(_info.updateUrl))location.href = _info.updateUrl(json);
                                else location.href = _info.updateUrl;
                            }
                            set_all_field_state("show");
                        }else{
                            clear_all_err_message();
                            show_err_request(status, json);
                            set_all_field_state("all-edit");
                        }
                        free();
                    })
                }else if(_data_set!==null&&$.isFunction(_data_set)) {
                    _data_set(result);
                    set_all_value(result);
                    set_all_field_state("show");
                    //尝试进行网页跳转。
                    if(_info.updateUrl!==null){
                        if($.isFunction(_info.updateUrl))location.href = _info.updateUrl(json);
                        else location.href = _info.updateUrl;
                    }
                    free();
                }else{throw "No useful delegate function or component."}
            }else{
                set_all_field_state("all-edit");
                free();
            }
        }
    };
    var submit_delete = function () {
        //提交一个删除请求, 发送一个delete。
        if(isfree()) {
            run();
            if(_rest_com!==null&&$.isFunction(_rest_com["delete"])) {
                _rest_com["delete"](function (success, status, json) {
                    if(success){
                        //提交成功之后会试图进行网页跳转。
                        if(_info.deleteUrl!==null) location.href = _info.deleteUrl;
                        else history.back();
                    }else{
                        clear_all_err_message();
                        show_err_request(status, json);
                    }
                    free();
                })
            }else if(_data_del!==null&&$.isFunction(_data_del)) {
                _data_del();
                //试图进行网页跳转。
                if(_info.deleteUrl!==null) location.href = _info.deleteUrl;
                free();
            }else{throw "No useful delegate function or component."}
        }
    };

    var load_data = function () {
        //这个函数将从数据源获取数据，并进行填充。
        set_panel("loading");
        if(_rest_com!==null&&$.isFunction(_rest_com["retrieve"])) {
            _rest_com["retrieve"](function (success, status, json) {
                if(success) {
                    //成功获取数据。
                    set_all_value(json);
                    set_panel("panel");
                }else{
                    set_loading_error(status, json);
                    set_panel("error");
                }
            })
        }else{
            var json = _data_get();
            set_all_value(json);
            set_panel("panel");
        }
    };

    //构建面板的显示元素 ==============================================================
    var build_title_div = function () {
        //构造title部分。
        return $('<div class="row mb-4"></div>')
            .append($('<div class="col"></div>').append($('<h2></h2>').text(_info.title)))
            .append($('<div class="col-auto"></div>').append(build_function_buttons()));
    };
    var build_function_buttons = function () {
        var group = $('<div class="btn-group"></div>');
        _func_btn_set.div = group;
        //TODO 功能按钮组
        if(_info.allowAllUpdate){
            _func_btn_set.all_edit_btn = $('<button class="btn btn-outline-secondary"></button>')
                .append($('<i class="fa fa-pencil-square-o"></i>'))
                .click(get_func_switch_edit());
            group.append(_func_btn_set.all_edit_btn);
        }
        if(_info.allowDelete){
            _func_btn_set.delete_btn = $('<button class="btn btn-danger" data-toggle="modal" data-target="#deleteModal"></button>')
                .append($('<i class="fa fa-trash"></i>'));
            group.append(_func_btn_set.delete_btn);
        }
        return group;
    };
    var build_panel_div = function () {
        //构造主面板。
        var _panel_div_col = $('<div class="col"></div>');
        _panel_div_col.append(build_table());
        _panel_div = $('<div class="row mt-5"></div>').append(_panel_div_col);
        return _panel_div;
    };
    var build_table = function () {
        var tbody = $('<tbody></tbody>');
        for(var i in _info.content) {
            var field = _info.content[i];
            var result = build_detail_field_td(i, field);
            _table_set[i] = result;
            //切换行默认所处的状态。
            if(field.defaultState!==null){
                if(field.defaultState === "show")set_field_state(i, "show");
                else if(field.defaultState === "edit" && field.partialWrite)set_field_state(i, "edit");
            }else {
                set_field_state(i, _all_field_state);
            }
            //输入默认值。
            if(field.defaultValue!==null)set_detail_field_value(field, result.read_ele, result.write_ele, field.defaultValue);
            tbody.append(result.tr);
        }
        return $('<table class="table"></table>').append(tbody);
    };
    var build_detail_field_td = function(index, field) {
        //这个函数会构造一个完整的行，可以直接安插进表格里，具备完整的功能和引用。
        var tr = $('<tr class="row"></tr>');

        var td_header = $('<td class="col-3"></td>');
        var label = $('<label class="font-weight-bold"></label>').text(field.header);
        td_header.append(label);

        var td_content = $('<td class="col-7"></td>');
        var readEle = build_detail_field_read(field);
        var writeEle = build_detail_field_write(field);
        var loadingDiv = $('<div class="progress m-5"></div>')
            .append($('<div class="progress-bar bg-secondary progress-bar-striped progress-bar-animated" style="width: 100%"></div>'));
        var errorDiv = $('<div class="row"></div>');
        td_content.append(readEle).append(writeEle).append(loadingDiv).append(errorDiv);

        var td_end = $('<td class="col-2"></td>');
        var updateBtn = $('<button class="btn btn-outline-secondary btn"></button>')
            .append($('<i class="fa fa-pencil"></i>'))
            .click(get_func_edit_btn(index));
        var submitBtn = $('<button class="btn btn-secondary btn"></button>')
            .append($('<i class="fa fa-check"></i>'))
            .click(get_func_submit_btn(index));
        td_end.append(updateBtn).append(submitBtn);

        tr.append(td_header).append(td_content).append(td_end);
        return {
            tr: tr,
            read_ele: readEle,
            write_ele: writeEle,
            error_div: errorDiv,
            update_btn: updateBtn,
            submit_btn: submitBtn,
            loading_div: loadingDiv
        };
    };
    var build_loading_div = function () {
        _loading_div = $('<div class="row"></div>')
            .append($('<div class="col"></div>')
                .append($('<div class="progress m-5"></div>')
                    .append($('<div class="progress-bar bg-secondary progress-bar-striped progress-bar-animated" style="width: 100%"></div>'))));
        return _loading_div;
    };
    var build_error_div = function () {
        _alert_div = $('<div class="alert alert-danger"></div>');
        _error_div = $('<div class="row"></div>')
            .append($('<div class="col"></div>')
                .append(_alert_div));
        return _error_div;
    };
    var build_bottom_error_div = function () {
        //构造底栏上关于错误提示框的一行。
        var div = $('<div class="col"></div>');
        _sum_error_div = div;
        return $('<div class="row"></div>').append(div);
    };
    var build_bottom_div = function () {
        //构造底部面板。
        var _bottom_div_col = $('<div class="col"></div>')
        if(_info.allowAllUpdate) {
            var submit_btn = $('<button class="btn btn-secondary btn-block mt-4">提交</button>');
            submit_btn.click(get_func_submit_all_btn());
            _func_btn_set.all_submit_btn = submit_btn;
        }
        _bottom_div_col.append(submit_btn);
        _bottom_div = $('<div class="row"></div>')
            .append($('<div class="col"></div>'))
            .append(_bottom_div_col)
            .append($('<div class="col"></div>'));
        return _bottom_div;
    };
    var build_delete_notice = function () {
        //构建删除时的提示框。
        _delete_modal = $('<div class="modal fade" id="deleteModal"></div>')
            .append($('<div class="modal-dialog modal-sm"></div>')
                .append($('<div class="modal-content"></div>')
                    .append($('<div class="modal-header"></div>')
                        .append($('<h4 class="modal-title">确认</h4>'))
                        .append($('<button type="button" class="close" data-dismiss="modal">&times;</button>')))
                    .append($('<div class="modal-body">确认要删除吗?</div>'))
                    .append($('<div class="modal-footer"></div>')
                        .append($('<button type="button" class="btn btn-danger" data-dismiss="modal">删除</button>').click(get_func_delete()))
                        .append($('<button type="button" class="btn btn-outline-secondary" data-dismiss="modal">取消</button>')))))
        return _delete_modal;
    };

    //设置面板的状态等工作 =============================================================
    var set_panel = function (name) {
        //调整面板的显示状态。在[loading|panel|error]之间切换。
        if(name === "loading") {
            _panel_div.hide();
            _loading_div.show();
            _error_div.hide();
            _sum_error_div.hide();
            _bottom_div.hide();
            _func_btn_set.div.hide();
        }else if(name === "error") {
            _panel_div.hide();
            _loading_div.hide();
            _error_div.show();
            _sum_error_div.hide();
            _bottom_div.hide();
            _func_btn_set.div.hide();
        }else {
            _panel_div.show();
            _loading_div.hide();
            _error_div.hide();
            _sum_error_div.show();
            _bottom_div.show();
            _func_btn_set.div.show();
        }
        _panel_state = name;
    };
    var set_loading_error = function (status, json) {
        //这个函数会配置错误面板。
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
        _alert_div.text("错误: " + message);
    };

    var set_field_state = function (index, state) {
        //这会将某一个元素行设定为指定的状态。包括[show|edit|all-edit|loading]。
        var s = _table_set[index];
        var field = _info.content[index];

        if(s){
            _table_state[index] = state;
            if(state === "show") {
                soh(field.readable, s.tr);
                show(s.read_ele);
                hide(s.write_ele);
                soh(field.writable&&field.partialWrite, s.update_btn);
                hide(s.submit_btn);
                hide(s.loading_div);
            }else if(state === "edit") {
                soh(field.readable||field.writable, s.tr);
                soh(!field.writable, s.read_ele);
                soh(field.writable, s.write_ele);
                hide(s.update_btn);
                soh(field.writable&&field.partialWrite, s.submit_btn);
                hide(s.loading_div);
            }else if(state === "all-edit"){
                soh(field.readable||field.writable, s.tr);
                soh(!field.writable, s.read_ele);
                soh(field.writable, s.write_ele);
                hide(s.update_btn);
                hide(s.submit_btn);
                hide(s.loading_div);
            }else {//loading
                show(s.tr);
                hide(s.read_ele);
                hide(s.write_ele);
                hide(s.update_btn);
                hide(s.submit_btn);
                show(s.loading_div);
            }
        }
    };
    var set_value = function (index, value) {
        var set = _table_set[index];
        var field = _info.content[index];
        set_detail_field_value(field, set.read_ele, set.write_ele, value);
    };
    var show_err_message = function (index, message) {
        //这会向指定的元素行推送一条错误。
        var err = $('<div class="alert alert-danger mt-2 alert-dismissable"></div>')
            .append($('<button type="button" class="close btn-sm" data-dismiss="alert">&times;</button>'))
            .append(message);
        if(index < 0) {
            _sum_error_div.append(err);
        }else if(index < _table_set.length){
            _table_set[index].error_div.append(err);
        }
    };
    var clear_err_message = function (index) {
        _table_set[index].error_div.html("");
    };

    var set_all_field_state = function (state) {
        _all_field_state = state;
        for(var i in _table_set)set_field_state(i, state);
        set_toolbar_state(state);
    };
    var set_toolbar_state = function (state) {
        //根据总的state，设置位于顶端工具栏内的edit|submit按钮的显示状态。
        if(state === "show") {
            hide(_func_btn_set.all_submit_btn);
            _func_btn_set.all_edit_btn.attr("class", "btn btn-outline-secondary btn-block")
        }else if(state === "all-edit") {
            show(_func_btn_set.all_submit_btn);
            _func_btn_set.all_edit_btn.attr("class", "btn btn-secondary btn-block")
        }
    };
    var collect_and_check = function (index) {
        var field = _info.content[index];
        var ele = _table_set[index].write_ele;
        try {
            return get_detail_field_value(field, ele);
        }catch(e){
            show_err_message(index, e);
            return undefined;
        }
    };
    var collect_all_and_check = function () {
        var err_happend = false;
        var json = {};
        for(var i in _info.content) {
            var field = _info.content[i];
            if(field.writable) {
                var value = collect_and_check(i);
                if(value!==undefined){
                    json[field.field] = value;
                }else{
                    err_happend = true;
                }
            }
        }
        if(err_happend)return undefined;
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
    var set_all_value = function (json) {
        for(var i in _info.content) {
            var field = _info.content[i];
            var set = _table_set[i];
            var value = json[field.field];
            if(value!==undefined) {
                set_detail_field_value(field, set.read_ele, set.write_ele, value);
            }
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
    var clear_all_err_message = function () {
        //这会清除所有的错误提示。
        for(var i in _table_set) _table_set[i].error_div.html("");
        _sum_error_div.html("");
    };

    //任务占用的操作 ==================================================================
    var isfree = function () {
        return !_running;
    };
    var run = function () {
        _running = true;
    };
    var free = function () {
        _running = false;
    };

    //一些辅助函数 ====================================================================
    var show = function (obj) {
        if(obj instanceof jQuery) {
            obj.show();
        }
    };
    var hide = function (obj) {
        if(obj instanceof jQuery){
            obj.hide();
        }
    };
    var soh = function (condition, obj) {
        if(obj instanceof jQuery){
            if(condition)obj.show();else obj.hide();
        }
    };

    return {
        info: function (info) {
            _info = info;
            //填充默认值。
            if(!("title" in _info))_info["title"] = null;
            if(!("defaultPanel" in _info))_info["defaultPanel"] = "loading";
            if(!("defaultState" in _info))_info["defaultState"] = "show";
            if(!("validate" in _info))_info["validate"] = null;
            if(!("content" in _info))_info["content"] = [];
            //追加错误处理数据。
            if(!("errorStatus" in _info))_info["errorStatus"] = {};
            if(!("errorKeyword" in _info))_info["errorKeyword"] = {};

            if(!("updateUrl" in _info))_info["updateUrl"] = null;
            if(!("deleteUrl" in _info))_info["deleteUrl"] = null;
            if(!("allowAllUpdate" in _info))_info["allowAllUpdate"] = true;
            if(!("allowPartialUpdate" in _info))_info["allowPartialUpdate"] = true;
            if(!("allowDelete" in _info))_info["allowDelete"] = true;
            for(var k in default_error_status_map)
                if(!(k in _info.errorStatus))_info.errorStatus[k] = default_error_status_map[k];
            for(k in default_error_keyword_map)
                if(!(k in _info.errorKeyword))_info.errorKeyword[k] = default_error_keyword_map[k];
            //处理每一个field内容的默认值。
            for(var i in _info.content) {
                var field = _info.content[i];
                if(!("type" in field))field["type"] = null;
                if(!("typeInfo" in field))field["typeInfo"] = {};
                if(!("validate" in field))field["validate"] = null;
                if(!("readable" in field))field["readable"] = true;
                if(!("writable" in field))field["writable"] = true;
                if(!("partialWrite" in field))field["partialWrite"] = true;
                if(!("defaultValue" in field))field["defaultValue"] = null;
                if(!("defaultState" in field))field["defaultState"] = null;
                initialization_detail_field_info(field);
                if(is_detail_field_read_only(field.type)){
                    field.readable = true;
                    field.writable = false;
                }
            }
            _all_field_state = _info.defaultState === "show"?"show":_info.defaultState === "edit"?"all-edit":null;

            _obj.append(build_title_div())
                .append(build_panel_div())
                .append(build_loading_div())
                .append(build_error_div())
                .append(build_bottom_error_div())
                .append(build_bottom_div())
                .append(build_delete_notice());
            set_panel(_info.defaultPanel);
            //不能在这里设定all state，会破坏每个元素独有的默认状态。
            set_toolbar_state(_all_field_state);
            return this;
        },
        dataGet: function (data_getter) {
            _data_get = data_getter;
            return this;
        },
        dataUpdate: function (data_setter) {
            _data_set = data_setter;
            return this;
        },
        dateDelete: function (data_taker) {
            _data_del = data_taker;
            return this;
        },
        rest: function (restful_component) {
            _rest_com = restful_component;
            return this;
        },
        load: load_data
    };
}

function initialization_detail_field_info(field) {
    var t;if(field.type === null) t = detail_fields[default_detail_field_elements]["init"];
    else if(field.type in detail_fields) t = detail_fields[field.type]["init"];
    else t = null;
    if(t&&$.isFunction(t)) t(field.typeInfo)
}
function build_detail_field_read(field) {
    //这个函数将会构造field所需的展示控件。
    if(!field.readable)return null;
    var ele;
    if(field.type === null) ele = detail_fields[default_detail_field_elements]["read"](field.typeInfo);
    else if(field.type in detail_fields) ele = detail_fields[field.type]["read"](field.typeInfo);
    else ele = null;
    return ele;
}
function build_detail_field_write(field) {
    //这个函数将会构造field所需的修改控件。
    if(!field.writable)return null;
    var ele;
    if(field.type === null) ele = detail_fields[default_detail_field_elements]["write"](field.typeInfo);
    else if(field.type in detail_fields) ele = detail_fields[field.type]["write"](field.typeInfo);
    else ele = null;
    return ele;
}
function get_detail_field_value(field, ele) {
    //这个函数会从field的write ele中提取所需要的值，并进行合法性检查。
    if(!field.writable)return undefined;
    var value;
    if(field.type === null) value = detail_fields[default_detail_field_elements]["get"](field.typeInfo, ele);
    else if(field.type in detail_fields) value = detail_fields[field.type]["get"](field.typeInfo, ele);
    else value = null;
    if(value !== null && field.validate !== null) value = field.validate(value);
    return value;
}
function set_detail_field_value(field, readEle, writeEle, value) {
    //这个函数会为field的ele赋值.为read组件和write组件同时赋值。如果存在不可读或不可写，对应的组件引用会为null。
    if(field.readable||field.writable) {
        if (field.type === null) detail_fields[default_detail_field_elements]["set"](field.typeInfo, readEle, writeEle, value);
        else if (field.type in detail_fields) detail_fields[field.type]["set"](field.typeInfo, readEle, writeEle, value);
    }
}
function is_detail_field_read_only(type) {
    //此类型的控件是否是只读的？
    var ret;
    if(type === null) ret = detail_fields[default_detail_field_elements]["readOnly"];
    else if(type in detail_fields) ret = detail_fields[type]["readOnly"];
    else ret = null;
    if(ret === true)return ret;
    else return false;
}
/** fields type的书写结构：
 * <typeName> : {
 *      init: function(typeInfo) 初始化类型参数。可以省略。
 *      read: function(typeInfo) 构造展示控件
 *      write: function(typeInfo) 构造修改控件
 *      get: function(typeInfo, ele) 从修改控件中取值
 *      set: function(typeInfo, readEle, writeEle, value) 修改控件显示的值
 *      readOnly: bool = false 只读控件。该控件默认是不可写的。此属性会在构造默认值时就被偷偷修改。这些控件不需要write|get方法。
 * }
 */
var detail_fields = {
    text: {
        init: function (info) {
            if(!("allowBlank" in info))info["allowBlank"] = true;
            if(!("allowNull" in info))info["allowNull"] = false;
        },
        read: function (info) {
            return $('<label></label>');
        },
        write: function (info) {
            var ret = $('<input type="text" class="form-control"/>');
            if(info){
                if(("placeholder" in info))ret.attr("placeholder", info["placeholder"]);
            }
            return ret;
        },
        get: function (info, ele) {
            var ret = ele.val().trim();
            if(info){
                if(info.length&&ret.length > info.length)throw "长度不能超过" + info.length + "位。";
                if((!info.allowBlank)&&ret.length === 0)throw "内容不能为空。";
                if(info.allowNull&&ret.length === 0)ret = null;
            }
            return ret;
        },
        set: function (info, readEle, writeEle, value) {
            if(readEle)readEle.text(value);
            if(writeEle)writeEle.val(value);
        }
    },
    password: {
        init: function (info) {
            if(!("allowBlank" in info))info["allowBlank"] = true;
            if(!("allowNull" in info))info["allowNull"] = false;
        },
        read: function (info) {
            return $('<label></label>');
        },
        write: function (info) {
            var ret = $('<input type="password" class="form-control"/>');
            if(info){
                if(("placeholder" in info))ret.attr("placeholder", info["placeholder"]);
            }
            return ret;
        },
        get: function (info, ele) {
            var ret = ele.val().trim();
            if(info){
                if(info.length&&ret.length > info.length)throw "长度不能超过" + info.length + "位。";
                if((!info.allowBlank)&&ret.length === 0)throw "内容不能为空。";
                if(info.allowNull&&ret.length === 0)ret = null;
            }
            return ret;
        },
        set: function (info, readEle, writeEle, value) {
            if(readEle){
                if(value instanceof String){
                    if(value.length > 0)readEle.text("******");
                    else readEle.text("");
                }
            }
            if(writeEle)writeEle.val(value);
        }
    },
    datetime: {
        init: function (info) {
            if(!("allowBlank" in info))info["allowBlank"] = true;
            if(!("allowNull" in info))info["allowNull"] = false;
            if(!("format" in info))info["format"] = "yyyy-mm-dd hh:ii:ss";
            if(!("view" in info))info["view"] = "minute";
            if(!("defaultView" in info))info["defaultView"] = "month";
        },
        read: function (info) {
            return $('<label></label>');
        },
        write: function (info) {
            var format = info.format;
            var view = datetimepicker_range_to_num(info.view);
            var defaultView = datetimepicker_range_to_num(info.defaultView);

            var show_content = $('<input class="form-control rounded-0" size="16" type="text" readonly>');
            info["hidden_show_content"] = show_content;
            var ret = $('<div class="btn-group input-append date form_datetime"></div>')
                .append(show_content)
                .append($('<button class="btn btn-secondary add-on"></button>')
                    .append($('<i class="fa fa-calendar"></i>')));
            ret.datetimepicker({
                format: format, todayBtn: true, pickerPosition: "bottom-left", minView: view, startView: defaultView,
                startDate: new Date("1970/1/1")
            });
            ret.datetimepicker('update');
            ret.on('changeDate', function (e) {
                info["hidden_now_value"] = fmt_dt_date(e.date, "yyyy-m-d h:i:s");
            });
            return ret;
        },
        get: function (info, ele) {
            var ret = info["hidden_now_value"];
            if(info){
                if((!info.allowBlank)&&ret===undefined)throw "内容不能为空。";
                if(info.allowNull&&ret.length === 0)ret = null;
            }
            return ret;
        },
        set: function (info, readEle, writeEle, value) {
            var format = info.format;

            var com_value = value instanceof Date?fmt_dt_date(value, format):fmt_dt_json(value, format);//将值转换为可读的标准格式。
            //更新write的值
            if(writeEle){
                info["hidden_show_content"].val(com_value); //修改input的值
                writeEle.datetimepicker('update'); //通知控件进行更新
                info["hidden_now_value"] = (value instanceof Date?fmt_dt_date:fmt_dt_json)(value, "yyyy-m-d h:i:s"); //修改藏在info内部的实际值
            }
            //更新read的值
            if(readEle)readEle.text(com_value);
        }
    }
    //TODO 做更多的预定义类型
};
var default_detail_field_elements = "text";
/** typeInfo文档：
 * text: {
 *      placeholder: string = undefined 在edit模式下写在文本框底
 *
 *      allowBlank: bool = true 是否允许内容为空
 *      allowNull: bool = false 是否允许null。在内容为空时，会返回null。
 *      length: int = undefined 给出长度限制
 * }
 *  password: {
 *      placeholder: string = undefined 为文本框添加底部提示文本
 *
 *      allowBlank: bool = true 是否允许内容为空
 *      allowNull: bool = false 是否允许null。在内容为空时，会返回null。
 *      length: int = undefined 给出长度限制
 *  }
 * datetime: { //default: Date
 *      allowBlank: bool = true 是否允许空。这将返回null。
 *      allowNull: bool = false 是否允许null。在内容为空时，会返回null。
 *      format: string = "yyyy-mm-dd hh:ii:ss" 默认的日期展示格式。
 *      view: string = 'minute' 可以展示的时间范围。可选[minute|hour|day|month|year]。
 *      defaultView: string = 'month' 默认展示的时间范围。
 *  }
 */

/**TODO detail页
 需要做一个匹配List<*>的东西。与create细节近似。
 需要做一个匹配模型引用的东西，结合list&create对应功能的特点。展示时直接展示json名称，修改时与create操作相仿。
 为了扩充功能，需要加入一个组件对应多个json field的功能。使field能传入数组就可以。
 */