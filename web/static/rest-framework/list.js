/** 用于创建对接REST风格的list列表的工具集。
 * 功能包括：
 * 1. 创建具有自定义特性的列表
 * 2. 具有分页功能
 * 3. 具有根据列排序、筛选的功能
 * 4. 具有搜索框
 */

/**构造list面板。可以将面板输出到指定jquery对象。
 * obj: 建议使用一个".col"的div面板。
 * info的构成：
 * {
 *      title: string = null 表格表头
 *      table: {
 *          striped: bool = true 为表格添加间隔条纹。
 *          small: bool = false 小号表格。
 *          responsive: bool = false 自适应表格。
 *      }
 *      defaultPanel: string = "init-loading" 表格默认的起始面板，用于提升微体验。
 *
 *      createUrl: string = null 启用标准的创建按钮，并给出导向url。
 *
 *      searchable: bool = true 开启搜索功能。
 *      pageable: bool = true 开启分页功能。启用该功能时，传入的数据必须符合分页标准。
 *      content: [ 列的配置列表。
 *          {
 *              header: string 表头名称。
 *              field: string 在json中的字段名称。
 *              type: string|function(json) = null 该列数据的处理类型。可以不写，然后按默认手段处理。也可以传入一个函数用作自定义处理。传入json的dataitem。
 *              typeInfo: object = null 附加给类型的额外内容。有些类型需要额外内容来处理其信息。
 *              sortable: bool = false 可以以该列为基准排序。
 *              link: string|function(json) = null 如果该元素的类型可使用超链接，就为其配置一个链接。也可以传入一个函数用作自定义链接处理，传入json的dataitem。
 *          }
 *      ],
 *      errorStatus: {
 *          <HTTP_STATUS: MESSAGE>
 *      },
 *      errorKeyword: {
 *          <KEYWORD: MESSAGE>
 *      }
 * }
 */
/** 数据源函数source的参数定义：
 * source_info: {
 *      search: string = null,
 *      [filter: object = null],
 *      order: string = null,
 *      pageFirst: int = 0,
 *      pageMaxResult: int = null,
 * },
 * source_action: function 当数据源处理完其数据请求之后，调用此函数，并传入数据。
 */
/** 页面的html结构：
 *  - obj.col被传入的顶级结构
 *      - title_div.row
 *          |title_div.col|function_button_div.col|
 *              - h2             - div.btn-group
 *      - panel_div.row [在loading/panel模式下]
 *          |table_div.col|
 *              - ...
 *      - loading_div.row [在loading模式下]
 *          |loading_div.col|
 *              - div.progress
 *      - error_div.row [在error模式下]
 *          |error_div.col|
 *              - div.alert
 */
function build_list(obj) {
    var _info = null; //构造的配置信息
    var _source = null; //数据源调用函数。
    var _restsource = null; //来自core的数据源。

    var _panel_state = null; //当前的面板状态。

    var _obj = obj; //构造面板的jquery object
    var _panel_div = null; //成功输出信息的面板。
    var _loading_div = null; //正在加载的面板。
    var _error_div = null; //错误的面板。

    var _function_btns = null; //功能按钮组。
    var _alert_div = null; //错误提示框。
    var _sort_btn_icon = []; //所有排序按钮的数组
    var _tbody = null; //表格体
    var _searchbox = null; //搜索框
    var _pagediv = null; //存放分页按钮的div
    var _pageinput = null; //存放分页输入框的input

    var PAGE_BTN_MAX = 5; //最多存在的分页页码数
    var DEFAULT_MAX_RESULT = 20; //默认最多一页的内容数

    var _sortstatus = []; //所有排序标记的状态。
    var _sortstack = []; //标记排序按钮点击顺序的列表。

    var _searchtext = null; //搜索内容
    var _pageindex = 0; //第一条记录的下标
    var _pagemax = DEFAULT_MAX_RESULT; //最大内容数量
    var _order = null; //排序内容
    var _filter = null; //筛选内容

    var _now_page = null; //当前页的页码。
    var _max_page = null; //最大页码。

    //获取各种用于表格功能的回调函数。
    var get_func_to_page_index = function(index) {
        //返回一个函数。该函数会使表格跳转到指定的页码。
        return function() {
            if(_panel_state!=="panel")return; //在没有处于面板状态下时，所有的表格功能函数全部被禁用。
            var i;
            if(index<1)i=1;else if(index>_max_page)i=_max_page;else i=index;
            //根据index和_pagemax，可以计算_pageindex.
            setpage((i - 1) * _pagemax);
            build();
        }
    };
    var get_func_to_page_prev = function () {
        return get_func_to_page_index(_now_page - 1);
    };
    var get_func_to_page_next = function () {
        return get_func_to_page_index(_now_page + 1);
    };
    var get_func_to_showall = function () {
        //返回一个函数，该函数会使列表查询全部内容而不进行分页。
        return function () {
            if(_panel_state!=="panel")return; //在没有处于面板状态下时，所有的表格功能函数全部被禁用。
            setpage(1);
            setpagemax(_max_page * _pagemax);
            build();
        }
    };
    var get_func_sort = function(index) {
        //返回一个函数，该函数会修改序列在index上的排序的状态。状态初始值为-(0)，在-(0)↑(+1)↓(-1)之间循环。
        return function() {
            if(_panel_state!=="panel")return; //在没有处于面板状态下时，所有的表格功能函数全部被禁用。
            if(_sortstatus[index]===0){
                _sortstatus[index] = 1;
                _sort_btn_icon[index].attr("class", "fa fa-sort-asc")
            }else if(_sortstatus[index]===1) {
                _sortstatus[index] = -1;
                _sort_btn_icon[index].attr("class", "fa fa-sort-desc")
            }else {
                _sortstatus[index] = 0;
                _sort_btn_icon[index].attr("class", "fa fa-sort")
            }
            //在排序栈中将本项放到最前面。
            for(var j in _sortstack) {
                if(_sortstack[j] === index){
                    _sortstack.splice(j, 1);
                    break;
                }
            }
            _sortstack.unshift(index);
            //生成新的order参数并再次构造。
            var arr = [];
            for(var k in _sortstack) {
                var i = _sortstack[k]; //从排序栈提取一个下标
                var field = _info.content[i];
                if(field.sortable&&_sortstatus[i]!==null&&_sortstatus[i]!==0){
                    arr.push((_sortstatus[i]>0?"":"-") + field.field);
                }
            }
            setorder(arr);
            build();
        }
    };
    var func_do = function(func) {
        return function () {
            if(_panel_state!=="panel")return;
            func();
        }
    };
    var func_goto = function (url) {
        return function () {
            if(_panel_state!=="panel")return;
            location.href = url;
        }
    };
    var do_search = function() {//提供给搜索框的回调函数，用于开始搜索指令。
        if(_panel_state!=="panel")return;
        var text = _searchbox.val().trim();
        setsearch(text);
        build();
    };

    //各种用于构建系统某一部分的函数。
    var build_title_div = function () {
        //构建标题行。
        //标题行包括的内容为大标题和自定义按钮。
        return $('<div class="row mb-4"></div>')
            .append($('<div class="col"></div>').append($('<h2></h2>').text(_info.title)))
            .append($('<div class="col-auto"></div>').append(build_function_buttons()));
    };
    var build_function_buttons = function () {
        //构造标题行上的功能按钮组。
        var group = $('<div class="btn-group"></div>');
        if(_info.createUrl!==null){
            group.append($('<button class="btn btn-outline-secondary"></button>')
                .append($('<i class="fa fa-file-text"></i>'))
                .click(func_goto(_info.createUrl)))
        }
        _function_btns = group;
        return group;
    };
    var build_error_div = function () {
        _alert_div = $('<div class="alert alert-danger"></div>');
        _error_div = $('<div class="row"></div>')
            .append($('<div class="col"></div>')
                .append(_alert_div));
        return _error_div;
    };
    var build_loading_div = function () {
        _loading_div = $('<div class="row"></div>')
            .append($('<div class="col"></div>')
                .append($('<div class="progress m-5"></div>')
                    .append($('<div class="progress-bar bg-secondary progress-bar-striped progress-bar-animated" style="width: 100%"></div>'))));
        return _loading_div;
    };
    var build_panel_div = function() {
        //构建主面板。
        var _panel_div_col = $('<div class="col"></div>');
        if(_info.searchable)_panel_div_col.append(build_search());
        _panel_div_col.append(build_table()).append(build_page());
        _panel_div = $('<div class="row"></div>')
            .append(_panel_div_col);
        return _panel_div;
    };
    var build_search = function () {
        //构造搜索框部分
        var grp = $('<div class="btn-group"></div>');
        var searchbox = $('<input type="text" class="rounded-0 form-control" placeholder="Search for..."/>');
        var searchbtn = $('<button class="btn btn-secondary" type="button"></button>').append($('<i class="fa fa-search"></i>'));
        _searchbox = searchbox;
        searchbtn.click(do_search);
        searchbox.keydown(function (event) { if(event.keyCode === 13)do_search() });
        return $('<div class="row mb-4"></div>').append(grp.append(searchbox).append(searchbtn));
    };
    var build_table = function() {
        //构造表格部分。返回表格的jQuery对象。
        //构造head
        var thead_tr = $('<tr></tr>');
        var thead = $('<thead></thead>').append(thead_tr);
        for(var i in _info.content){
            var field = _info.content[i];

            var label = $('<label></label>').text(field.header);
            var sortbtn;var icon;if(field.sortable) {
                icon = $('<i class="fa fa-sort"></i>');
                sortbtn = $('<button class="btn btn-outline-secondary btn-sm ml-1"></button>').append(icon).click(get_func_sort(i))
            }else{sortbtn = null; icon = null;}
            _sort_btn_icon[i] = icon;
            if(field.sortable)_sortstatus[i] = 0;else _sortstatus[i] = null;

            var th = $('<th></th>').append(label).append(sortbtn);
            thead_tr.append(th);
        }
        //构造body
        var tbody = $('<tbody></tbody>');
        _tbody = tbody;

        //构造table并返回
        var table_class = "table";
        if(_info.table.striped) table_class += " table-striped";
        if(_info.table.small) table_class += " table-sm";
        if(_info.table.responsive) table_class += " table-responsive";
        var table = $('<table></table>').attr("class", table_class);
        return $('<div class="row"></div>').append(table.append(thead).append(tbody));
    };
    var build_page = function () {
        //构造pagediv
        _pagediv = $('<div class="row"></div>');
        return _pagediv;
    };

    //用于抛出实际数据的函数。
    var build_error = function (status, json) {
        /**
         * 在这里简述错误信息的获取方式。
         * 首先可以知道，在抛出错误时，ajax会返回【错误状态码statuscode】和【json讯息】。
         * 从info中，能够得到两个错误处理器，【errorStatus】和【errorKeyword】。
         *      errorStatus在不提供时，会使用默认的那一套。
         *  现在，接收到了一个新的错误。
         *      先检查json讯息是否具有keyword项，且errorKeyword中有此映射。
         *          有，那么使用errorKeyword中的此项。
         *          没有，检查errorStatus是否有对应的status映射。
         *              有，使用errorStatus中的此项。
         *              没有，检查json中是否有message讯息。
         *                  有，使用此讯息。
         *                  没有，检查有没有默认的状态码名字。
         *                      有，使用其名字。
         *                      没有，使用状态码。
         */
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
    var build_data = function (data) {
        //开始按照数据构造表格内容。
        //data的内容：{count: 本api的总条目数量, index: 本页的开头下标, content: 内容}
        var content;if(_info.pageable)content = data.content;else content = data;
        for(var i in content) {
            var dataitem = content[i];
            var tr = $('<tr></tr>');
            for(var j in _info.content) {
                var field = _info.content[j];
                var fieldvalue = dataitem[field.field];
                var td = $('<td></td>');
                //为td填充内容
                var tdcontent = build_list_field(field, dataitem);
                tr.append(td.append(tdcontent));
            }
            _tbody.append(tr);
        }
        if(_info.pageable) {
            //添加表格的分页模块。
            var now_page_num = Math.floor(data.index / _pagemax) + 1; //计算当前页的标准页码，从1开始。
            var max_page_num = Math.ceil(data.count / _pagemax); //计算最大页码。
            _now_page = now_page_num;
            _max_page = max_page_num;
            if(max_page_num > 1){//页数大于1才有展示分页模块的必要。
                var pageul = $('<div class="btn-group"></div>');

                var firstbtn = $('<button class="btn btn-outline-secondary"></button>').append($('<i class="fa fa-angle-double-left"></i>')).click(get_func_to_page_index(1));
                var prevbtn = $('<button class="btn btn-outline-secondary"></button>').append($('<i class="fa fa-angle-left"></i>')).click(get_func_to_page_prev());
                if(now_page_num <= 1) {
                    firstbtn.attr("class", "btn btn-outline-secondary disabled");
                    prevbtn.attr("class", "btn btn-outline-secondary disabled");
                }
                pageul.append(firstbtn).append(prevbtn);

                var page_btn_begin;
                var page_btn_end;
                if(max_page_num > PAGE_BTN_MAX) { //分页的页数超出展示范畴，那么只展示最近的一部分页码。
                    if(now_page_num > PAGE_BTN_MAX/2 && now_page_num < max_page_num - PAGE_BTN_MAX/2) {
                        page_btn_begin = now_page_num - Math.floor(PAGE_BTN_MAX/2);
                        page_btn_end = page_btn_begin + PAGE_BTN_MAX - 1;
                    }else if(now_page_num <= PAGE_BTN_MAX/2) {
                        page_btn_begin = 1;
                        page_btn_end = PAGE_BTN_MAX;
                    } else {
                        page_btn_end = max_page_num;
                        page_btn_begin = page_btn_end - PAGE_BTN_MAX + 1;
                    }
                }else{//展示全部页码。
                    page_btn_begin = 1;
                    page_btn_end = max_page_num;
                }
                for(i = page_btn_begin; i <= page_btn_end; i++) {
                    if(now_page_num === i) {
                        var pageinput = $('<input class="form-control rounded-0 text-white btn-secondary bg-secondary" style="width: 45px;text-align:center"/>')
                            .attr("value", now_page_num)
                            .keypress(function (event) {
                                if(event.keyCode === 13){get_func_to_page_index(parseInt(_pageinput.val()))()}
                            });
                        _pageinput = pageinput;
                        pageul.append(pageinput);
                    }else{
                        var pagebtn = $('<button class="btn btn-outline-secondary"></button>').text(i).click(get_func_to_page_index(i));
                        pageul.append(pagebtn);
                    }
                }

                var nextbtn = $('<button class="btn btn-outline-secondary"></button>').append($('<i class="fa fa-angle-right"></i>')).click(get_func_to_page_next());
                var lastbtn = $('<button class="btn btn-outline-secondary"></button>').append($('<i class="fa fa-angle-double-right"></i>')).click(get_func_to_page_index(max_page_num));
                if(now_page_num >= max_page_num) {
                    nextbtn.attr("class", "btn btn-outline-secondary disabled");
                    lastbtn.attr("class", "btn btn-outline-secondary disabled");
                }
                pageul.append(nextbtn).append(lastbtn);
                //添加【显示全部】按钮
                var showallbtn = $('<button class="btn btn-outline-secondary">显示全部</button>').click(get_func_to_showall());
                //构造div
                _pagediv
                    .append($('<div class="col"></div>').append(showallbtn))
                    .append($('<div class="col-auto rounded-right"></div>').append(pageul));
            }
        }
    };

    //设置显示的部分。
    var set_panel = function (name) {
        //切换前台显示的面板。
        if(name === "error"){
            _loading_div.hide();
            _panel_div.hide();
            _error_div.show();
            _function_btns.hide();
        }
        else if(name === "loading"){
            _loading_div.show();
            _panel_div.show();
            _error_div.hide();
            _function_btns.hide();
        }else{
            _loading_div.hide();
            _panel_div.show();
            _error_div.hide();
            _function_btns.show();
        }
        _panel_state = name;
    };

    //用于设置查询选项的函数。
    var setsearch = function (text) {
        //设定搜索内容。
        _searchtext = text;
    };
    var setpage = function(page_index) {
        //设定分页的页码。
        _pageindex = page_index;
    };
    var setpagemax = function (page_max) {
        _pagemax = page_max;
    };
    var setorder = function (order_array) {
        //设定排序内容。数组为一系列排序的基准字符串。
        _order = order_array;
    };
    var setfilter = function (filter_map) {
        //设定筛选器。map为筛选器的一系列映射。
        _filter = filter_map;
    };

    //功能函数。
    var clearconfig = function () {
        //清除所有的选项数据。
        _searchtext = null;
        _pageindex = 0;
        _pagemax = DEFAULT_MAX_RESULT;
        _order = null;
        _filter = null;
    };
    var clear = function () {
        //清除内容面板上遗留的上一次的数据。
        _tbody.html("");
        _pagediv.html("");
        _pageinput = null;
    };
    var build = function () {
        var requestinfo = {};
        if(_searchtext!==null)requestinfo["search"] = _searchtext;
        if(_order!==null)requestinfo["order"] = _order;
        if(_pageindex!==null)requestinfo["pageFirst"] = _pageindex;
        if(_pagemax!==null)requestinfo["pageMaxResult"] = _pagemax;
        if(_filter!==null) for(var i in _filter) {
            requestinfo[i] = _filter[i];
        }
        clear();
        set_panel("loading");
        //数据构造函数。
        if($.isFunction(_restsource)){
            _restsource(requestinfo, function (success, status, json) {
                if(success) {
                    build_data(json);
                    set_panel("panel");
                }else{
                    build_error(status, json);
                    set_panel("error");
                }
            })
        }else{
            _source(requestinfo, build_data);
            set_panel("panel");
        }
    };

    return {
        /** 传入构造参数，同时开始构造模板。
         */
        info: function (info) {
            _info = info;
            //第一遍处理的同时，也会填充info的默认值。
            if(!("title" in _info))_info["title"] = null;
            if(!("table" in _info))_info["table"] = {};
            //处理table的参数
            if(!("striped" in _info.table))_info.table["striped"] = true;
            if(!("small" in _info.table))_info.table["small"] = false;
            if(!("responsive" in _info.table))_info.table["responsive"] = false;

            if(!("defaultPanel" in _info))_info["defaultPanel"] = "loading";
            if(!("searchable" in _info))_info["searchable"] = true;
            if(!("pageable" in _info))_info["pageable"] = true;
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
                if(!("sortable" in field))field["sortable"] = false;
                if(!("link" in field))field["link"] = null;
            }
            //处理顶栏按钮的默认值。
            if(!("createUrl" in _info))_info["createUrl"] = null;
            //构造面板
            _obj.append(build_title_div()).append(build_panel_div()).append(build_loading_div()).append(build_error_div());
            //切换默认面板
            set_panel(_info.defaultPanel);
            return this;
        },
        /** 传入数据源，为构造数据建立基础。
         * 数据源应当传入一个函数，接受一组参数。
         */
        data: function (data_getter) {
            _source = data_getter;
            return this;
        },
        /** 直接传入一个REST风格的回调函数作为源。这将与CORE组件耦合，并与错误处理组件耦合。
         */
        rest: function(restful_getter) {
            //restful_getter: function(requestinfo: {...}, delegate: function(bool, int, json))
            _restsource = restful_getter;
            return this;
        },
        /** 开始构造内容。构造内容的依据为数据源、搜索框信息、排序信息、筛选信息、分页信息。
         */
        build: build,

        clear: clear,
        clearconfig: clearconfig,

    }
}

function build_list_field(field, dataitem) {
    var link;
    if($.isFunction(field.link)){//调用回调函数构造链接。
        link = field.link(dataitem);
    }else if(field.link!==null){//直接构造链接。
        link = field.link;
    }else{//不使用链接。
        link = null;
    }
    var jqobj;
    if(field.type !== null && $.isFunction(field.type)){
        jqobj = field.type(dataitem);
    }else if(field.type === null){
        jqobj = list_field_elements[default_list_field_elements](field.typeInfo, dataitem[field.field], link);
    }else if(field.type in list_field_elements){
        jqobj = list_field_elements[field.type](field.typeInfo, dataitem[field.field], link);
    }else{
        jqobj = null;
    }
    //最后做一个智能判断。
    if(jqobj === null){
        return null;
    }else if(!(jqobj instanceof jQuery)){
        //如果最后得到的不是jquery对象，就使用默认方法将其包装成jquery对象。
        return (link===null)?$('<label></label>').text(jqobj):$('<a></a>').attr("href", link).text(value);
    }else{
        return jqobj;
    }
}
/** list的field的构建模式。
 * <typeName> : function(typeInfo: any, value: any, link: string = null) 给出该构建模式的构建函数。
 *                      传入的参数为【类型参数】、【内容值】和【链接】。链接为null时表示不使用链接。
 *                      也可以不理会链接，这样就不会触发链接参数。
 *                      返回值一般理应返回jquery控件，不过也可以返回其他值，会被自动包装成标签|超链接。
 */
var list_field_elements = {
    text: function (info, value, link) {
        return (link===null)?$('<label></label>').text(value):$('<a></a>').attr("href", link).text(value);
    },
    datetime: function (info, origin_value, link) {
        var fmt = "yyyy-mm-dd hh:ii:ss";
        if(info){
            if("format" in info) fmt = info["format"];
        }
        var value = fmt_dt_json(origin_value, fmt);
        return (link===null)?$('<label></label>').text(value):$('<a></a>').attr("href", link).text(value);
    }
};
var default_list_field_elements = "text";
/**typeInfo 文档：
 * text: {
 * }
 * datetime: {
 *      format: string = "yyyy-mm-dd hh:ii:ss" 展示的时间日期格式。按示例所示填写即可。
 * }
 */
/**TODO list页
 需要做一个匹配List<*>的东西，将一个列表平稳展示。这个简单。
 不做匹配模型引用的东西，这个不值。让后端在json数据里直接附加需要用来展示给前端的信息就可以。
 */
/**TODO 做表格的列选择功能。
 *      1. 给出一个组件，可以任意/在指定范围内选择表格列的显示;
 *      2. 给出一个综合性的列切换组件，可以在预先组织好的几组列里切换，用于展示有太多列，但是能分组的信息。
 * */

