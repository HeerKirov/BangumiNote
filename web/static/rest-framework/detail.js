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
            if((_info.content[index] instanceof Object) && _info.content[index].writable)set_field_state(index, "edit");
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
            if(field instanceof Object) {
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
                                if(field.field in json)set_and_refresh_value(index, field.field===null?json:json[field.field], result_json[field.field]);
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
                        set_and_refresh_value(index, result, result);
                        set_field_state(index, "show");
                        free();
                    }else{throw "No useful delegate function or component."}
                }else{
                    set_field_state(index, "edit");
                    free();
                }
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
                            set_and_refresh_all_value(json, result);
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
                    set_and_refresh_all_value(result, result);
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
                    set_and_refresh_all_value(json, null);
                    set_panel("panel");
                }else{
                    set_loading_error(status, json);
                    set_panel("error");
                }
            })
        }else{
            var json = _data_get();
            set_and_refresh_all_value(json, null);
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
        var tbody = $('<div class="p-1"></div>');
        var collapse_index = 0;
        var analysing_collapse = null;
        for(var i in _info.content) {
            var field = _info.content[i];
            if(field instanceof Object) {
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
                if(analysing_collapse === null) tbody.append(result.tr);
                else analysing_collapse.append(result.tr);
            }else if(field.substring(0, 2) === "hr") {
                if(analysing_collapse === null) tbody.append($('<hr/>'));
                else analysing_collapse.append($('<hr/>'))
            }else if(field.substring(0, 8) === "collapse") {
                var name = field.length > 9 ? field.substring(9): "";
                var get_func_collapse = function (icon) {
                    var collapse_state = true;
                    return function () {
                        collapse_state = !collapse_state;
                        icon.attr("class", collapse_state?"fa fa-caret-down":"fa fa-caret-up");
                    };
                };
                var collapse_icon = $('<i class="fa fa-caret-down"></i>');
                var collapse_btn = $('<button class="btn btn-block btn-light text-dark" style="text-align: left" data-toggle="collapse"></button>')
                    .attr("data-target", "#rest-collapse-" + collapse_index)
                    .append(name + " ")
                    .append(collapse_icon)
                    .click(get_func_collapse(collapse_icon));
                var collapse_panel = $('<div class="collapse"></div>')
                    .attr("id", "rest-collapse-" + collapse_index);
                analysing_collapse = collapse_panel;
                collapse_index++;
                tbody.append($('<div class="row"></div>')
                    .append(collapse_btn))
                    .append($('<div class="row mt-1 mb-1"></div>')
                        .append($('<div class="col"></div>')
                            .append(collapse_panel)));
            }else if(field === "end") {
                analysing_collapse = null;
            }
        }
        return tbody;
    };
    var build_detail_field_td = function(index, field) {
        //这个函数会构造一个完整的行，可以直接安插进表格里，具备完整的功能和引用。
        var tr = $('<div class="row mt-2 mb-2"></div>');

        var td_header = $('<div class="col-lg-3 col-md-4 col-xs-11"></div>');
        var label = $('<label class="font-weight-bold"></label>').text(field.header);
        td_header.append(label);

        var td_content = $('<div class="col-lg-8 col-md-7"></div>');
        var readEle = build_detail_field_read(field);
        var writeEle = build_detail_field_write(field);
        var loadingDiv = $('<div class="progress m-5"></div>')
            .append($('<div class="progress-bar bg-secondary progress-bar-striped progress-bar-animated" style="width: 100%"></div>'));
        var errorDiv = $('<div class="row"></div>');
        td_content.append(readEle).append(writeEle).append(loadingDiv).append(errorDiv);

        var td_end = $('<div class="col-1"></div>');
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

        if(field instanceof Object && s){
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
    var set_and_refresh_value = function (index, value, origin_value) {
        var set = _table_set[index];
        var field = _info.content[index];
        if(field instanceof Object) {
            refresh_detail_field(field, set.read_ele, set.write_ele, origin_value);
            set_detail_field_value(field, set.read_ele, set.write_ele, value);
        }
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
        if(field instanceof Object) {
            var ele = _table_set[index].write_ele;
            try {
                return get_detail_field_value(field, ele);
            }catch(e){
                show_err_message(index, e);
                return undefined;
            }
        }
    };
    var collect_all_and_check = function () {
        var err_happend = false;
        var json = {};
        for(var i in _info.content) {
            var field = _info.content[i];
            if(field instanceof Object && field.writable) {
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
    var set_and_refresh_all_value = function (json, origin_json) {
        for(var i in _info.content) {
            var field = _info.content[i];
            if(field instanceof Object) {
                var set = _table_set[i];
                var origin_value = (origin_json)?origin_json[field.field]:undefined;
                var value = field.field===null?json:json[field.field];
                if(value!==undefined) {
                    refresh_detail_field(field, set.read_ele, set.write_ele, origin_value);
                    set_detail_field_value(field, set.read_ele, set.write_ele, value);
                }
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
                if(field instanceof Object) {
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
    console.log("set " + field.field);
    //这个函数会为field的ele赋值.为read组件和write组件同时赋值。如果存在不可读或不可写，对应的组件引用会为null。
    if(field instanceof Object &&(field.readable||field.writable)) {
        if (field.type === null) detail_fields[default_detail_field_elements]["set"](field.typeInfo, readEle, writeEle, value);
        else if (field.type in detail_fields) detail_fields[field.type]["set"](field.typeInfo, readEle, writeEle, value);
    }
}
function refresh_detail_field(field, readEle, writeEle, origin_value) {
    if(field instanceof Object) {
        var f = (field.type === null)? detail_fields[default_detail_field_elements]["refresh"] :
            (field.type in detail_fields)? detail_fields[field.type]["refresh"] : undefined;
        if($.isFunction(f))f(field.typeInfo, readEle, writeEle, origin_value);
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
 *      refresh: function(typeInfo, readEle, writeEle, origin_value) 在每次提交成功之后，会试图执行一次刷新函数。value是提交时的数据。
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
    },
    number: {
        init: function (info) {
            if(!("allowBlank" in info))info["allowBlank"] = true;
            if(!("allowNull" in info))info["allowNull"] = false;
            if(!("arrowButton" in info))info["arrowButton"] = false;
            if(!("narrow" in info))info["narrow"] = false;
            if(!("allowFloat" in info))info["allowFloat"] = true;
        },
        read: function (info) {
            return $('<label></label>');
        },
        write: function (info) {
            var number_change = function(delita) {
                return function () {
                    var value;
                    try{value = this.get(info, ele);}catch(ex){return;}
                    var min = undefined, max = undefined;
                    if(info) {
                        if("min" in info) min = info["min"];
                        if("max" in info) max = info["max"];
                    }
                    if(delita>0&&(max===undefined||value<max)) value += delita;
                    else if(delita<0&&(min===undefined||value>min)) value += delita;
                    ret.val(value);
                };
            };
            var ret = $('<input type="text" class="form-control"/>');
            var arrowButton = false;
            if(info){
                if(("placeholder" in info))ret.attr("placeholder", info["placeholder"]);
                if(("arrowButton" in info)&&info["arrowButton"])arrowButton = true;
                if(("narrow" in info)&&info["narrow"])ret.attr("style", "width: 40%; text-align: center");
            }
            var ele;
            if(arrowButton) {
                ele = $('<div class="btn-group"></div>')
                    .append($('<button type="button" class="btn btn-outline-secondary"></button>')
                        .append($('<i class="fa fa-caret-left"></i>'))
                        .click(number_change(-1)))
                    .append(ret.attr("class", "form-control rounded-0"))
                    .append($('<button type="button" class="btn btn-outline-secondary"></button>')
                        .append($('<i class="fa fa-caret-right"></i>'))
                        .click(number_change(1)));
            }else {
                ele = $('<div class="btn-group"></div>').append(ret);
            }
            return ele;
        },
        get: function (info, ele) {
            var ret = ele.find(".form-control").val();
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
        set: function (info, readEle, writeEle, value) {
            if(readEle)readEle.text(value);
            if(writeEle)writeEle.find(".form-control").val(value);
        }
    },
    mapping: {
        init: function (info) {
            if(!("map" in info))info["map"] = {};
            if(!("allowNull" in info))info["allowNull"] = false;
        },
        read: function (info) {
            return $('<label></label>');
        },
        write: function (info) {
            var ret = $('<select class="form-control"></select>');
            for(var i in info.map) {
                ret.append($('<option></option>').text(info.map[i].header));
            }
            return ret;
        },
        get: function (info, ele) {
            var retIndex = ele[0].selectedIndex;
            if((!info.allowNull)&&isNaN(retIndex))throw "选择的内容不能为空。";
            return info.map[retIndex].value;
        },
        set: function (info, readEle, writeEle, value) {
            for(var i in info.map) if (info.map[i].value === value) {
                writeEle[0].selectedIndex = i;
                readEle.text(info.map[i].header);
                return;
            }
            writeEle[0].selectedIndex = -1;
            readEle.text("");
        }
    },
    foreignChoice: {
        _index: 0,
        init: function (info) {
            if(!("many" in info))info["many"] = false;
            if(!("link" in info))info["link"] = null;
            if(!("allowForeign" in info))info["allowForeign"] = true;
            if(!("allowCustom" in info))info["allowCustom"] = true;
            if(!("allowNull" in info))info["allowNull"] = false;
            if(!("showContent" in info))info["showContent"] = null;
            if(!("isValueContent" in info))info["isValueContent"] = function (json, goal) {
                if(json instanceof Object && goal instanceof Object && ("id" in json) && ("id" in goal))return json.id === goal.id;
                else return json === goal;
            };
            if(!("isNewValue" in info))info["isNewValue"] = function (value) {
                return (value instanceof Object)
            };
            if(info.allowForeign) {
                if(!("foreignRequest" in info))throw "Param 'foreignRequest' is necessary.";
                if(!("foreignHeader" in info))throw "Param 'foreignHeader' is necessary.";
                if(!("foreignValue" in info))throw "Param 'foreignValue' is necessary.";
            }
            if(info.allowCustom) {
                if(!("customContent" in info))throw "Param 'customContent' is necessary.";
                else {
                    for(var i in info.customContent) {
                        var field = info.customContent[i];
                        if(field instanceof Object) {
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
                    }
                }
            }
        },
        read: function (info) {
            return $('<div></div>');
        },
        write: function (info) {
            //功能函数。
            var refresh_panel_state = function () {
                //在执行完面板变动之后，刷新面板和选项卡按钮的显示状态。
                info.tab_bar.html("");
                var any_exists = false;
                if(info.allowForeign)info.tab_bar.append(info.tab_bar_btn_select);
                for(var i in info.tab_bar_btn_customs) if(info.tab_bar_btn_customs[i] !== null){
                    any_exists = true;
                    info.tab_bar.append(info.tab_bar_btn_customs[i]);
                }
                if(info.many||!any_exists)info.tab_bar.append(info.tab_bar_btn_add.click(add_button_click));
                info.tab_panel.html("");
                if(info.allowForeign)info.tab_panel.append(info.tab_panel_select);
                for(i in info.tab_panel_customs) if(info.tab_panel_customs[i] !== null){
                    info.tab_panel.append(info.tab_panel_customs[i]);
                    info.tab_panel_customs[i].find("#del-btn").click(get_del_button_click(i));
                }
            };
            var get_del_button_click = function (index) {
                return function () {
                    if(index>=0 && index<info.custom_count) {
                        info.tab_bar_btn_customs[index] = null;
                        info.tab_panel_customs[index] = null;
                        info.tab_panel_custom_eles[index] = null;
                        refresh_panel_state();
                    }
                };
            };
            var del_all_panel = function () {
                for(var i = 0; i < info.custom_count; ++i) {
                    info.tab_bar_btn_customs[i] = null;
                    info.tab_panel_customs[i] = null;
                    info.tab_panel_custom_eles[i] = null;
                }
                refresh_panel_state();
            };
            var add_button_click = function() {
                //按下add按钮，添加新的custom面板。
                var tab_bar_btn = $('<li class="nav-item"></li>')
                    .append($('<a class="nav-link" data-toggle="tab"></a>')
                        .text("新建项" + (info.custom_count + 1))
                        .attr("href", "#foreign-choice-" + info.index + "-custom-" + info.custom_count));
                info.tab_bar_btn_customs[info.custom_count] = tab_bar_btn;
                var tab_bar_panel = $('<div class="container tab-pane p-3 border rounded-bottom"></div>').attr("id", "foreign-choice-" + info.index + "-custom-" + info.custom_count);
                //构造自定义面板的内容并记录
                var eles = [];
                var tbody = $('<div class="col"></div>');
                for(var i in info.customContent) {
                    var field = info.customContent[i];
                    if(field instanceof Object) {
                        var header = $('<label></label>').text(field.header);
                        var tdcontent = build_create_field(field);
                        eles[i] = tdcontent;
                        var field_ele = $('<div class="row mt-1 mb-1"></div>')
                            .append($('<div class="col-lg-2 col-md-3 col-xs-12"></div>')
                                .append(header))
                            .append($('<div class="col-lg-10 col-md-9"></div>')
                                .append(tdcontent));
                        tbody.append(field_ele);
                    }//TODO else string
                }
                var del_button = $('<button id="del-btn" class="btn btn-danger"></button>')
                    .append("删除 ")
                    .append($('<i class="fa fa-trash"></i>'));

                tab_bar_panel.append($('<div class="row p-1"></div>')
                    .append(tbody));
                tab_bar_panel.append($('<div class="row pt-1"></div>')
                    .append($('<div class="col"></div>'))
                    .append($('<div class="col-auto"></div>')
                        .append(del_button)));
                info.tab_panel_custom_eles[info.custom_count] = eles;
                info.tab_panel_customs[info.custom_count] = tab_bar_panel;
                info.custom_count ++;
                refresh_panel_state();
            };
            //真正的build的业务逻辑。
            info["index"] = ++this._index;
            info["custom_count"] = 0;
            info["tab_bar_btn_customs"] = [];
            info["tab_panel_customs"] = [];
            info["tab_panel_custom_eles"] = [];
            //有关请求的线程限制参数。
            info["request_refresh"] = function () {
                //使控件刷新其持有的数据列表。
                del_all_panel();
                info.request_running = true;
                info.foreignRequest(function (data) {
                    info.select_box.html("");
                    for(var i in data) {
                        var header_text = info.foreignHeader(data[i]);
                        var option = $('<option></option>').attr("id", i).attr("value", i).text(header_text);
                        info.select_box.append(option);
                    }
                    info.select_box[0].selectedIndex = -1;
                    info.select_data = data;
                    info.request_running = false;
                    if(info.request_write_waiting !== null) {
                        info.write_set(info.request_write_waiting.writeEle, info.request_write_waiting.value);
                        info.request_write_waiting = null;
                    }
                });
            };
            info["request_write"] = function(writeEle, value) {
                //在线程安全的情况下提交一组对write的修改。
                //内容会等待，直到request running任务结束才会执行。
                if(info.request_running) {
                    info.request_write_waiting = {writeEle: writeEle, value: value};
                }else{
                    info.write_set(writeEle, value);
                }
            };
            info["request_write_waiting"] = null; //内部参数。如果write提交遇到了阻塞，会放在这里面等待完成。
            info["request_running"] = false; //这个参数给set看的，让它知道当前是否在执行内容刷新任务。
            info["write_set"] = function (writeEle, value) {
                //立刻修改write组件展示的内容。不建议直接调用，建议由refresh函数和submit函数来调用。
                if(info.many) {
                    info.select_box[0].selectedIndex = -1;
                    for(var k in value) {
                        for(i in info.select_data) {
                            if(info.isValueContent(value[k], info.select_data[i])) {
                                info.select_box.find("#" + i).attr("selected", true);
                                break;
                            }
                        }
                    }
                }else{
                    //为了找出哪一个select项是配对项，需要做一个遍历。
                    info.select_box[0].selectedIndex = -1;
                    for(i in info.select_data) {
                        if(info.isValueContent(value, info.select_data[i])) {
                            info.select_box[0].selectedIndex = i;
                            break;
                        }
                    }
                }
            };
            var all_panel = $('<div class="col"></div>');
            if(info.allowForeign) {
                //构造select控件。
                info["select_box"] = $('<select class="form-control"></select>');
                info["select_data"] = null;
                if(info.many)info.select_box.attr("multiple", true);
                //请求数据源。这里的代码和request_refresh不太一样。
                info.request_running = true;
                info.foreignRequest(function (data) {
                    for(var i in data) {
                        var header_text = info.foreignHeader(data[i]);
                        var option = $('<option></option>').attr("id", i).attr("value", i).text(header_text);
                        info.select_box.append(option);
                    }
                    info.select_box[0].selectedIndex = -1;
                    info.select_data = data;
                    info.request_running = false;
                    if(info.request_write_waiting !== null) {
                        info.write_set(info.request_write_waiting.writeEle, info.request_write_waiting.value);
                        info.request_write_waiting = null;
                    }
                });
            }else{
                info["select_box"] = null;
            }
            if(info.allowCustom) {
                //只有允许自定义时，才会显示tab。如果没有自定义，只剩一个select，是没必要构造一堆额外的东西的。
                //构造tab顶栏
                var tab_bar = $('<ul class="nav nav-tabs" role="tablist"></ul>');
                all_panel.append(tab_bar);
                info["tab_bar"] = tab_bar;
                //在允许Foreign时，添加SELECT的选项卡按钮
                if(info.allowForeign) {
                    info["tab_bar_btn_select"] = $('<li class="nav-item"></li>').append($('<a class="nav-link active" data-toggle="tab">已有项</a>').attr("href", "#foreign-choice-" + info.index + "-select"))
                }
                //添加ADD按钮。
                info["tab_bar_btn_add"] = $('<li class="nav-item"></li>')
                    .append($('<button class="btn btn-success btn-sm ml-2"></button>')
                        .append($('<i class="fa fa-plus"></i>')));

                //构造tab选项卡区域
                var panel = $('<div class="tab-content"></div>');
                all_panel.append(panel);
                info["tab_panel"] = panel;
                //在允许Foreign时，将SELECT选项卡加入，并添加select控件。
                if(info.allowForeign) {
                    var select_panel = $('<div class="container tab-pane active p-3 border rounded-bottom"></div>').attr("id", "foreign-choice-" + info.index + "-select");
                    info["tab_panel_select"] = select_panel;
                    select_panel.append(info.select_box);
                }
                refresh_panel_state();//初始化刷新。
            }else{//在不允许自定义时，只展示select面板。
                all_panel.append(info.select_box);
            }
            return $('<div class="row"></div>').append(all_panel);
        },
        get: function (info, ele) {
            //准备提取值。
            //首先把所有来自select的值和来自custom的值分别提取出来。
            var value_select = [];
            if(info.allowForeign) {
                if(info.many) {
                    var options = info.select_box.find("option:selected");
                    for(var i = 0; i < options.length; ++i) {
                        var index = options[i].value;
                        value_select.push(info.foreignValue(info.select_data[index]));
                    }
                }else {
                    index = info.select_box[0].selectedIndex;
                    if(index >= 0) value_select.push(info.foreignValue(info.select_data[index]));
                }
            }
            //提取来自custom的值。提取时，仍然会执行validate检查。
            var value_custom = [];
            if(info.allowCustom) {
                for(i in info.tab_panel_custom_eles) {
                    var eles = info.tab_panel_custom_eles[i];
                    if(eles != null) {
                        var json = {};
                        var err = "";
                        var err_happend = false;
                        for(var j in info.customContent) {
                            var field = info.customContent[j];
                            if(field instanceof Object) {
                                try {
                                    json[field.field] = check_create_field(field, eles[j]);
                                }catch(e){
                                    if(!err_happend)err_happend = true;
                                    else err += "\n";
                                    err += field.header + ": " + e;
                                }
                            }
                        }
                        if(err_happend)throw err;
                        else value_custom.push(json);
                    }
                }
            }
            //之后根据allow和many按照优先级准备返回。
            if(info.many) {
                return value_select.concat(value_custom);
            }else{
                //在单值返回的情况下，优先返回custom的值。
                if(value_custom.length > 0) return value_custom[0];
                else if(value_select.length > 0) return value_select[0];
                else if(info.allowNull) return null;
                else throw "此项的值不能为空。";
            }
        },
        set: function (info, readEle, writeEle, value) {
            //设置值有点麻烦。
            if(info.many) {
                //多值模式下，value应当是数组。
                readEle.html("");
                var first = true;
                for(var i in value) {
                    if(first)first = false;else readEle.append(", ");
                    var str = (value[i] !== null) ? info.showContent(value[i]) : "";
                    var link = null;
                    if($.isFunction(info.link))link = info.link(value[i]);
                    else if(info.link !== null)link = info.link;
                    if(link !== null)readEle.append($('<a></a>').attr("href", link).text(str));
                    else readEle.append(str);
                }
                readEle.append()
            }else{
                //单值模式下，value是一个单独的值。
                readEle.html("");
                str = (value !== null) ? info.showContent(value) : "";
                link = null;
                if($.isFunction(info.link))link = info.link(value);
                else if(info.link !== null)link = info.link;
                if(link !== null)readEle.append($('<a></a>').attr("href", link).text(str));
                else readEle.append(str);
            }
            info.request_write(writeEle, value);
        },
        refresh: function (info, readEle, writeEle, origin_value) {
            //为了节省资源，只在value中包含新建的资源时刷新。
            if(info.allowForeign && origin_value) {
                if(info.many) {
                    for(var i in origin_value) {
                        if(info.isNewValue(origin_value[i])) {
                            info.request_refresh();
                            break;
                        }
                    }
                }else{
                    if(info.isNewValue(origin_value)){
                        info.request_refresh();
                    }
                }
            }
        }
    },
    constLink: {
        init: function (info) {
            if(!("text" in info))info["text"] = null;
            if(!("link" in info))info["link"] = null;
        },
        read: function(info) {
            var ret = $('<a></a>').text(info.text);
            if(!$.isFunction(info.link)&&info.link!==null)ret.attr("href", info.link);
            return ret;
        },
        set: function(info, readEle, writeEle, value) {
            if($.isFunction(info.link)) {
                readEle.attr("href", info.link(value))
            }
        },
        readOnly: true
    }
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
 *  number: { //default: number
 *      placeholder: string = undefined
 *      arrowButton: bool = false 添加工具按钮。
 *      narrow: bool = false 把框变成窄框并内容居中。
 *
 *      allowBlank: bool = true 是否允许空。
 *      allowNull: bool = false 是否允许为null。这将在内容为空时返回null。
 *      min: number = undefined 最小值。
 *      max: number = undefined 最大值。
 *      allowFloat: bool = true 允许浮点数。
 *  }
 *  mapping: {
 *      map: json = [{header: <string>, value: <string>}] 映射的内容。
 *
 *      allowNull: bool = false 是否允许不选择项。如果有默认值，这一条是不会达成的。
 *  }
 *  foreignChoice: {
 *      allowNull: bool = false 在单值的情况下，是否允许空值出现。
 *      many: bool = false 允许多值。
 *      allowForeign: bool = true 启用外部列表回调。
 *      allowCustom: bool = true 启用自定义面板。
 *      foreignRequest: function(function(json)) 回调函数，直接调用此函数取得默认列表参数。要求不直接返回值，而是通过传入的回调函数参数返回。
 *      foreignHeader: function(json) 用于生成在select内展示的标题。
 *      foreignValue: function(json) 用于生成准备提交的数据。
 *      customContent: [{}] 自定义项的子项。内容可以填写与CREATE面板其他组件相同的结构。
 *      showContent: function(json) 生成read模式下展示用的内容。
 *      isValueContent: function(json, goal) 进行对比判断，判断传给write函数的某个json值是否与select列表中取得的值相等。默认的对比函数将对比二者的id，然后再比对二者自身。
 *      isNewValue: function(value) 用在refresh中，判断一个数据是否是新建资源。默认判断其是否不是object。
 *      link: string|function(json) = null 是否将read模式下的文本转换为链接。
 *  }
 *  constLink: {
 *      text: string 展示的常值内容。
 *      link: string|function(json) 构成的链接。如果链接是函数，会在每次刷新时重构一次。
 *  }
 */

/**TODO detail页
 需要做一个匹配List<*>的东西。与create细节近似。
 需要做一个匹配模型引用的东西，结合list&create对应功能的特点。展示时直接展示json名称，修改时与create操作相仿。
 为了扩充功能，需要加入一个组件对应多个json field的功能。使field能传入数组就可以。
 */