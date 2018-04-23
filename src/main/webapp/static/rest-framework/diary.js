/**此脚本处理所有与Diary Web显示有关的组件。
 */

var diary = {
    /**构造diary web页面的总框架，自动包含所有必须的元素。
     * 包含的功能点：
     * 1. list显示。包含若干ROW。能分别进行详细处理。
     * 2. 搜索。按照关键字进行搜索。这个操作在本地进行。
     * 3. 排序。按照既定方案进行复杂排序。这个操作在本地进行。方案包括[名称排序|更新时间排序|更新周数排序*|待观看|建序][name/update_time/weekday/has_next/create]
     * 4. 按照条件筛选。这个操作在本地进行。条件包括[全部|进行中*|更新中|有存货|已归档][all/going/updating/watchable/complete]
     * 5. 刷新。这会重新获取list数据源。
     */
    root: function(rest, detail_rest) {
        var doFilter = function (originList, filterValue, keyword) {
            var ret = [];
            for(var i in originList) {
                var data = originList[i].getProperty();
                //执行查找条件。泛搜索的关键词在name中匹配。
                var leastOnce = false;
                if(keyword === null)leastOnce = true;
                else for(var k in keyword) {
                    if(data.name.indexOf(keyword[k]) > 0) {
                        leastOnce = true;
                    }
                }
                if(!leastOnce)continue; //没有至少一个关键词配对时，跳过去。
                //执行筛选条件。
                var filterTrue = false;
                if(filterValue === "going") {
                    //当data.is_completed === false时，判定为进行中。
                    if(!data.is_completed) filterTrue = true;
                }else if(filterValue === "updating") {
                    //当publish < total 且至少存在1个plan时，判定为还在更新。
                    if((data.publish_episode < data.total_episode) && (data.publish_plan.length > 0)) filterTrue = true;
                }else if(filterValue === "watchable") {
                    //当finished < publish时，判定为有存货。
                    if(data.finished_episode < data.publish_episode) filterTrue = true;
                }else if(filterValue === "complete") {
                    //当data.is_completed === true时，判定为归档。
                    if(data.is_completed) filterTrue = true;
                }else filterTrue = true;
                if(!filterTrue)continue;
                //最后还没有被跳过去的可以加入。
                ret.push(originList[i]);
            }
            return ret;
        };
        var doSort = function (list, sortValue) {
            /*排序方案：
                1. 名称排序： 按照data.name字典序排序。次一级的按照id排序。
                2. 更新时间排序：按照下次更新时间排序。
                                    没有下次更新时间的，有Next的排在前面。
                                        最次按照createTime排序。
                3. 更新周数排序：计算下次更新时间的weekday值，并按周一~周日排序，同值的按照当日更新时间排序。
                                    没有下次更新时间的，有Next的排在前面。
                                        最次按照createTime排序。
                4. 待观看排序：按照publish-finished降序排序。
                                    该数值相同时，按照createTime排序。
                5. 建序排序：完全按照createTime排序。
             */
            var xor = function (a, b) {
                return (a && (!b)) || ((!a) && b);
            };
            var getWeekdayValue = function (date) {
                var day = date.getDay();
                if (day === 0) day = 7;
                return date.getMinutes() + date.getHours() * 60 + day * 60 * 24;
            };
            var sortFunction = (sortValue === "update_time") ? function (a, b) {
                var tA = a.sort.time;
                var tB = b.sort.time;
                var cA = a.sort.create_time;
                var cB = b.sort.create_time;
                var nA = a.sort.next;
                var nB = b.sort.next;
                return (xor(tA !== null, tB !== null)) ? ((tA !== null) ? -1 : 1) :
                    (tA !== null && tB !== null) ? ((tA < tB) ? -1 : 1) :
                        (xor(nA, nB)) ? (nA ? -1 : 1) :
                            (cA === cB) ? 0 :
                                (cA < cB) ? -1 : 1;
            } : (sortValue === "weekday") ? function (a, b) {
                var tA = a.sort.time;
                var tB = b.sort.time;
                var cA = a.sort.create_time;
                var cB = b.sort.create_time;
                var nA = a.sort.next;
                var nB = b.sort.next;
                return (xor(tA !== null, tB !== null)) ? ((tA !== null) ? -1 : 1) : //desc, not null在前
                    (tA !== null && tB !== null) ? ((tA < tB) ? -1 : 1) ://asc
                        (xor(nA, nB)) ? (nA ? -1 : 1) ://有next在前
                            (cA === cB) ? 0 :
                                (cA < cB) ? -1 : 1;//asc
            } : (sortValue === "has_next") ? function (a, b) {
                var nA = a.sort.next;
                var nB = b.sort.next;
                var cA = a.sort.create_time;
                var cB = b.sort.create_time;
                return (nA !== nB) ? ((nA > nB) ? -1 : 1) : //desc
                    (cA === cB) ? 0 :
                        (cA < cB) ? -1 : 1; //asc
            } : (sortValue === "create") ? function (a, b) {
                var cA = a.sort.create_time;
                var cB = b.sort.create_time;
                return (cA === cB) ? 0 : (cA < cB) ? -1 : 1; //asc
            } : function (a, b) {//name
                return (a.sort.name === b.sort.name) ? 0 : (a.sort.name < b.sort.name) ? -1 : 1;
            };
            //根据排序类型，为排序单元添加所需要的数据。
            for (var i in list) {
                var item = list[i];
                var sortSet = {};
                item["sort"] = sortSet;
                var data = item.getProperty();
                if (sortValue === "update_time") {
                    sortSet["time"] = (data.publish_plan.length > 0) ? new Date(data.publish_plan[0]).getTime() : null;
                    sortSet["next"] = (data.publish_episode - data.finished_episode) > 0;
                    sortSet["create_time"] = new Date(data.create_time).getTime();
                } else if (sortValue === "weekday") {
                    sortSet["time"] = (data.publish_plan.length > 0) ? getWeekdayValue(new Date(data.publish_plan[0])) : null;
                    sortSet["next"] = (data.publish_episode - data.finished_episode) > 0;
                    sortSet["create_time"] = new Date(data.create_time).getTime();
                } else if (sortValue === "has_next") {
                    sortSet["next"] = data.publish_episode - data.finished_episode;
                    sortSet["create_time"] = new Date(data.create_time).getTime();
                } else if (sortValue === "create") {
                    sortSet["create_time"] = new Date(data.create_time).getTime();
                } else {
                    sortSet["name"] = data.name;
                }
            }
            list.sort(sortFunction);
            //清除排序数据。
            for (i in list) list[i].sort = undefined;
        };

        var buildHtml = function () {
            var ret = $('<div class="col"></div>')
                .append($('<div class="row"></div>')
                    .append($('<div class="col"></div>')
                        .append($('<h2>日记</h2>'))))
                .append($('<div class="row mt-4"></div>')
                    .append($('<div class="col-12 col-sm-6 btn-group mt-2"></div>')
                        .append($('<input id="search_box" class="form-control rounded-0" placeholder="Search for..."/>'))
                        .append($('<button id="search_button" class="btn btn-secondary"><i class="fa fa-search"></i></button>')))
                    .append($('<div class="col"></div>'))
                    .append($('<div class="col-auto btn-group mt-2"></div>')
                        .append(filterButton.obj)
                        .append(sortButton.obj)
                        .append($('<button id="refresh_button" class="btn btn-secondary"><i class="fa fa-refresh"></i></button>'))))
                .append($('<div id="div_panel" class="row"></div>')
                    .append($('<div id="row_list" class="col pt-4"></div>')))
                .append($('<div id="div_loading" class="row" style="display: none"></div>')
                    .append($('<div class="col"></div>'))
                    .append($('<div class="col-auto"></div>')
                        .append($('<i class="fa fa-spin fa-circle-o-notch fa-3x m-5"></i>')))
                    .append($('<div class="col"></div>')))
                .append($('<div></div>')
                    .append(modal.obj));
            ret.find("#search_box").keydown(function (event) { if(event.keyCode === 13){
                var value = this.value.trim();
                if(value === "")value = null;
                setSearch(value);
            } });
            ret.find("#search_button").click(function () {
                var value = ret.find("#search_box").val().trim();
                if(value === "")value = null;
                setSearch(value);
            });
            ret.find("#refresh_button").click(refresh);
            return ret;
        };
        var setSearch = function (keyword) {
            if(keyword !== null) searchKeyword = keyword.split(' ');
            else searchKeyword = null;
            draw();
        };
        var setSort = function (value) {
            sortState = value;
            draw();
        };
        var setFilter = function (value) {
            filterState = value;
            draw();
        };
        var refresh = function () {
            //从list源更新数据，然后刷入。
            if(refreshingFlag === false) {
                refreshingFlag = true;
                refreshButton.attr("disabled", true);
                var div_panel= obj.find("#div_panel");
                var div_loading = obj.find("#div_loading");
                var div_row_list = obj.find("#row_list");
                div_row_list.html("");
                div_panel.hide();
                div_loading.show();
                if(rest !== null && rest !== undefined) {
                    rest.request(null, function (success, status, data) {
                        rowList = [];
                        if(success){
                            for(var i in data.content) {
                                rowList.push(diary.row({
                                    rest: detail_rest(data.content[i]),
                                    data: data.content[i],
                                    modal: modal
                                }))
                            }
                            draw();
                        }else{
                            var msg;
                            if("message" in data) msg = status + ": " + data.message;
                            else msg = "错误 " + status;
                            div_row_list.append($('<div class="alert alert-danger alert-dismissable">')
                                .append($('<button type="button" class="close" data-dismiss="alert">&times;</button>'))
                                .append(msg));
                        }
                        refreshButton.attr("disabled", false);
                        refreshingFlag = false;
                        div_panel.show();
                        div_loading.hide();
                    })
                }else{
                    refreshButton.attr("disabled", false);
                    refreshingFlag = false;
                    div_panel.show();
                    div_loading.hide();
                }
            }
        };
        var draw = function () {
            //负责将list内的row元素重新刷入。
            //不管数据更新。
            //需要处理筛选和排序等操作。
            var div_row_list = obj.find("#row_list");
            for(var i in rowList) {
                rowList[i].obj.detach();
            }
            //筛选结果.
            currentRowList = doFilter(rowList, filterState, searchKeyword);
            //排序.
            doSort(currentRowList, sortState);
            //输出显示。
            for(i in currentRowList) {
                div_row_list.append(currentRowList[i].obj);
            }
        };
        //jQuery对象和custom对象
        var sortButton = diary.dropdownButton({
            select: [
                {name: "名称排序", title: "名称", value: "name"},
                {name: "更新时间排序", title: "更新时间", value: "update_time"},
                {name: "周历排序", title: "周历", value: "weekday"},
                {name: "待看排序", title: "待看", value: "has_next"},
                {name: "建序排序", title: "建序", value: "create"}
            ],
            defaultValue: "weekday",
            icon: "sort",
            change: setSort
        });
        var filterButton = diary.dropdownButton({
            select: [
                {name: "全部", value: "all"},
                {name: "进行中", value: "going"},
                {name: "更新中", value: "updating"},
                {name: "有存货", value: "watchable"},
                {name: "已归档", value: "complete"}
            ],
            defaultValue: "going",
            icon: "filter",
            change: setFilter
        });
        var modal = diary.modalDialog();
        var obj = buildHtml();
        var refreshButton = obj.find("#refresh_button");

        //后台数据
        var sortState = "weekday";
        var filterState = "going";
        var searchKeyword = null;

        var rowList = [];
        var currentRowList = [];

        //防多线程标记
        var refreshingFlag = false;

        refresh();
        return {
            obj: obj
        }
    },
    /**构造一个新的行组件。
     * data: 符合要求的diary api item。
     * rest: 符合要求的rest组件.
     * modal: 可以随时调用的modal组件，存在时会用于二重确认。
     * */
    row: function (info) {
        var buildHtml = function () {
            dateList = diary.dateList();
            var ret = $('<div class="card mt-2 mb-2 mb-sm-1 mb-lg-0"></div>')
                .append($('<div class="row card-body"></div>')
                    .append($('<div class="col"></div>')
                        .append($('<div class="row"></div>')
                            .append($('<div class="col-12 col-lg"></div>')
                                .append($('<h4 id="label_title" class="edit-state-read">Title</h4>'))
                                .append($('<input id="input_title" class="form-control edit-state-write" style="display: none"/>')))
                            .append($('<div class="col-12 col-lg-3 col-sm"></div>')
                                .append($('<span id="content_update_week" class="badge badge-info"></span>')))
                            .append($('<div class="col-12 col-lg-2 col-sm"></div>')
                                .append($('<small id="content_next"></small>')))
                            .append($('<div class="col col-lg-2"></div>')
                                .append($('<small id="content_publish"></small>')))))
                    .append($('<div class="col-2 col-md-1 mr-2 mr-md-4"></div>')
                        .append($('<div class="row"></div>')
                            .append($('<div class="col col-lg-2 mr-1 mb-1 mb-lg-0"></div>')
                                .append($('<button id="hand_button" class="btn btn-sm btn-link mr-2 text-info"><i class="fa fa-hand-o-right"></i></button>')))
                            .append($('<div class="col col-lg-2 ml-1 mt-1 mt-lg-0"></div>')
                                .append($('<button class="btn btn-sm btn-link mr-2" data-toggle="collapse"><i class="fa fa-info"></i></button>').attr("data-target", "#collapse-" + data.id)))))
                    .append($('<div class="col-12"></div>')
                        .append($('<div class="collapse"></div>').attr("id", "collapse-" + data.id)
                            .append($('<div class="row mt-3"></div>')
                                .append($('<div class="col-12 col-md-4 m-2"></div>')
                                    .append($('<div class="row mb-1"></div>')
                                        .append($('<div class="col-5">已看完</div>'))
                                        .append($('<div class="col-7"></div>')
                                            .append($('<label id="label_finished" class="edit-state-read"></label>'))
                                            .append($('<input id="input_finished" class="form-control edit-state-write" style="display: none"/>'))))
                                    .append($('<div class="row mb-1"></div>')
                                        .append($('<div class="col-5">已发布</div>'))
                                        .append($('<div class="col-7"></div>')
                                            .append($('<label id="label_publish" class="edit-state-read"></label>'))
                                            .append($('<input id="input_publish" class="form-control edit-state-write" style="display: none"/>'))))
                                    .append($('<div class="row mb-1"></div>')
                                        .append($('<div class="col-5">总数量</div>'))
                                        .append($('<div class="col-7"></div>')
                                            .append($('<label id="label_total" class="edit-state-read"></label>'))
                                            .append($('<input id="input_total" class="form-control edit-state-write" style="display: none"/>'))))
                                )
                                .append($('<div class="col-12 col-md-4 m-2"></div>')
                                    .append($('<table class="table table-sm edit-state-read"></table>')
                                        .append($('<thead></thead>')
                                            .append($('<tr></tr>')
                                                .append($('<th>更新时间</th>'))))
                                        .append($('<tbody id="table_update_read"></tbody>')))
                                    .append(dateList.obj.attr("id", "table_update_write").attr("class", "edit-state-write").attr("style", "display: none"))
                                ))
                            .append($('<div class="row"></div>')
                                .append($('<div id="div_alert" class="col-12 col-md"></div>'))
                                .append($('<div class="col-12 col-md-auto btn-group"></div>')
                                    .append($('<button id="edit_button" class="btn btn-outline-secondary">编辑 <i class="fa fa-edit"></i></button>'))
                                    .append($('<button id="delete_button" class="btn btn-danger">删除 <i class="fa fa-trash"></i></button>'))))))
                );
            ret.find("#edit_button").click(function () {
                setEditState(!editState);
            });
            ret.find("#delete_button").click(function () {
                if(modal) {
                    modal.openDialog({
                        title: "删除",
                        content: "确认要删除吗？<p><small>删除行为不可撤销。</small>",
                        button_content: "确定 <i class='fa fa-trash'></i>",
                        button_class: "btn-danger",
                        delegate: doDelete
                    })
                }else {
                    doDelete();
                }
            });
            ret.find("#hand_button").click(doIncrease);
            return ret;
        };
        var addWarning = function (msg) {
            var div = obj.find("#div_alert");
            div.html("");
            if(msg instanceof  Array) {
                for(var i = 0; i < msg.length; ++i){
                    div.append($('<div class="alert alert-warning alert-dismissable">')
                        .append($('<button type="button" class="close" data-dismiss="alert">&times;</button>'))
                        .append(msg[i]))
                }
            }else{
                div.append($('<div class="alert alert-warning alert-dismissable">')
                    .append($('<button type="button" class="close" data-dismiss="alert">&times;</button>'))
                    .append(msg))
            }
        };
        var setEditState = function (edit) {
            //切换内容的编辑状态。同时可能触发提交动作。
            if(edit) {//切换编辑
                editState = edit;
                obj.find(".edit-state-read").hide();
                obj.find(".edit-state-write").show();
                obj.find("#edit_button").attr("class", "btn btn-secondary").html('提交 <i class="fa fa-check"></i>');
            }else{//提交
                //提交需要封锁整个card，并等待返回。
                var value = getValue();
                if(value !== undefined) {
                    obj.attr('disabled', true);
                    rest.update(value, false, function (success, status, data) {
                        if(success) {
                            setProperty(data);
                            editState = edit;
                            obj.find(".edit-state-read").show();
                            obj.find(".edit-state-write").hide();
                            obj.find("#edit_button").attr("class", "btn btn-outline-secondary").html('编辑 <i class="fa fa-edit"></i>');
                        }else{
                            if("message" in data) {
                                addWarning(status + ": " + data.message);
                            }else{
                                addWarning("发生错误 " + status);
                            }
                        }
                        obj.attr('disabled', false);
                    });
                }
            }
        };
        var setProperty = function (newData) {
            data = newData;
            //title
            obj.find("#label_title").text(data.name);
            obj.find("#input_title").val(data.name);
            //三维
            obj.find("#label_finished").text(data.finished_episode);
            obj.find("#input_finished").val(data.finished_episode);
            obj.find("#label_publish").text(data.publish_episode);
            obj.find("#input_publish").val(data.publish_episode);
            obj.find("#label_total").text(data.total_episode);
            obj.find("#input_total").val(data.total_episode);
            //更新时间列表
            var table_update_read = obj.find("#table_update_read");
            table_update_read.html("");
            var max = data.publish_plan.length > 4 ? 4 : data.publish_plan.length;
            for(var i = 0; i < max; ++i) {
                var value = fmt_dt_json(data.publish_plan[i], "yyyy年mm月dd日 hh:ii:ss");
                table_update_read.append($('<tr></tr>').append($('<td></td>').text(value)));
            }
            dateList.setProperty({maxCount: data.total_episode - data.publish_episode, values: data.publish_plan});
            //hand按钮可用性。
            obj.find("#hand_button").attr('disabled', data.finished_episode >= data.publish_episode);
            //更新标志文本。
            //Next content会在finished_episode < publish_episode时，展示下一话序列号。
            //在finished == total时，显示已完成。
            obj.find("#content_next").html((data.finished_episode<data.publish_episode)?('Next <b>第' + (data.finished_episode + 1) + '话</b>'):
                (data.finished_episode >= data.total_episode)?'已完成 √':'');
            //Publish content在publish<total时，展示[更新至第X话],在publish>=total时展示[全X话].
            obj.find("#content_publish").html((data.publish_episode<data.total_episode)?
                ('更新至第<span class="badge badge-pill badge-info">' + data.publish_episode + '</span>话'):
                '全<span class="badge badge-pill badge-secondary">' + data.total_episode + '</span>话');
            //Update Week content复杂许多。
            //它的展示仅取plan的第1项。在plan没有项目时不予展示。
            //对于此项，将其转换为自然语言。
            //      如果时间在本周，展示【本周X hh:ii更新】.
            //      如果时间在下周，展示【下周X hh:ii更新】.
            //      超出范围，如果在本年内，展示【M月D日 hh:ii更新】.且使用sec颜色。
            //      超出本年，展示【YYYY年M月D日 hh:ii更新】且使用sec颜色。
            var content_update_week = obj.find("#content_update_week");
            if(data.publish_plan.length > 0) {
                var today = new Date();
                var date = new Date(data.publish_plan[0]);
                var minus = week_minus(date, today);
                if(minus === 0) content_update_week.text("本" + weekday_name(date.getDay()) + " " + fmt_dt_date(date, "hh:ii") + "更新").attr("class", "badge badge-info");
                else if(minus === 1) content_update_week.text("下" + weekday_name(date.getDay()) + " " + fmt_dt_date(date, "hh:ii") + "更新").attr("class", "badge badge-info");
                else if(date.getFullYear() === today.getFullYear()) content_update_week.text(fmt_dt_date(date, "m月d日 hh:ii") + "更新").attr("class", "badge badge-secondary");
                else content_update_week.text(fmt_dt_date(date, "yyyy年m月d日 hh:ii") + "更新").attr("class", "badge badge-secondary");
            }else{
                content_update_week.text("");
            }
        };
        var getProperty = function () {
            return data;
        };
        var getValue = function () {
            //提取当前的数据，并构成json。
            var name = obj.find("#input_title").val();
            var total_episode = parseInt(obj.find("#input_total").val());
            var publish_episode = parseInt(obj.find("#input_publish").val());
            var finished_episode = parseInt(obj.find("#input_finished").val());
            var publish_plan = dateList.getValues();
            if(name === "")addWarning("标题不能为空。");
            else if(isNaN(total_episode)||isNaN(publish_episode)||isNaN(finished_episode))addWarning("请输入合法的数字。");
            else if(total_episode < 1)addWarning("总数量不能低于1。");
            else if(publish_episode < 0)addWarning("已发布数量不能低于0。");
            else if(finished_episode < 0)addWarning("已看完数量不能低于0。");
            else if(publish_episode > total_episode)addWarning("已发布数量不能高于已看完。");
            else if(finished_episode > publish_episode)addWarning("已看完数量不能高于已发布。");
            else return {
                name: name,
                total_episode: total_episode,
                publish_episode: publish_episode,
                finished_episode: finished_episode,
                publish_plan: publish_plan
            };
            return undefined;
        };
        var doDelete = function () {
            obj.attr('disabled', true);
            rest["delete"](function (success, status, data) {
                if(success) {
                    obj.remove();
                }else{
                    if("message" in data) {
                        addWarning(status + ": " + data.message);
                    }else{
                        addWarning("发生错误 " + status);
                    }
                }
                obj.attr('disabled', false);
            });
        };
        var doIncrease = function () {
            //提交需要封锁整个card，并等待返回。
            obj.attr('disabled', true);
            rest.update({increase_finished: true}, false, function (success, status, data) {
                if(success) {
                    setProperty(data);
                }else{
                    if("message" in data) {
                        addWarning(status + ": " + data.message);
                    }else{
                        addWarning("发生错误 " + status);
                    }
                }
                obj.attr('disabled', false);
            })
        };
        //数据区
        var data = info["data"]; //当前呈现的数据
        var rest = info["rest"]; //用于REST交互的组件

        var editState = false; //当前是否处于编辑状态。

        //Object区
        var modal = info["modal"];
        var dateList = null; //写元素。必须放在root之前。
        var obj = buildHtml(); //root

        setProperty(data);
        return {
            obj: obj,
            setProperty: setProperty,
            getProperty: getProperty
        }
    },
    /**构造一个用于处理多日期列表的专用列表。
     * 仅用于write状态。
     * info: 用于设置初始状态。：{maxCount: Number, values: []}
     */
    dateList: function(info) {
        var buildHtml = function () {
            return $('<div></div>')
                .append($('<div class="row"></div>')
                    .append($('<div class="col"></div>')
                        .append($('<select id="select_list" multiple class="form-control"></select>'))))
                .append($('<div class="row mt-1"></div>')
                    .append($('<div class="col btn-group"></div>')
                        .append($('<button id="add_new_button" class="btn col btn-outline-primary">新项 <i class="fa fa-list-ol"></i></button>'))
                        .append($('<button id="add_weekday_button" class="btn col btn-outline-info">周更 <i class="fa fa-calendar"></i></button>'))
                        .append($('<button id="trash_item_button" class="btn col-auto btn-danger"><i class="fa fa-trash"></i></button>')))
                )
                .append($('<div id="add_tab" class="row mt-1 card" style="display: none"></div>')
                    .append($('<div class="col card-body" id="add_new_tab" style="display: none"></div>')
                        .append($('<div id="add_new_picker" class="btn-group input-append date form_datetime"></div>')
                            .append($('<input size="16" class="form-control rounded-0" type="text" value="" readonly>'))
                            .append($('<button class="btn btn-secondary add-on"><i class="fa-calendar fa"></i></button>'))))
                    .append($('<div class="col card-body" id="add_weekday_tab" style="display: none"></div>')
                            .append($('<div class="row"></div>')
                                .append($('<div class="col-12 col-sm-5"></div>')
                                    .append($('<label>初始时间</label>')))
                                .append($('<div class="col-12 col-sm-7"></div>')
                                    .append($('<div id="add_weekday_picker" class="btn-group input-append date form_datetime"></div>')
                                        .append($('<input size="16" class="form-control rounded-0" type="text" value="" readonly>'))
                                        .append($('<button class="btn btn-secondary add-on"><i class="fa-calendar fa"></i></button>')))))
                            .append($('<div class="row"></div>')
                                .append($('<div class="col-12 col-sm-5"></div>')
                                    .append($('<label>数量</label>')))
                                .append($('<div class="col-12 col-sm-7"></div>')
                                    .append($('<input id="add_weekday_count" class="form-control"/>'))))
                            .append($('<div class="row"></div>')
                                .append($('<div class="col-12 col-sm-5"></div>')
                                    .append($('<label>间隔天数</label>')))
                                .append($('<div class="col-12 col-sm-7"></div>')
                                    .append($('<input id="add_weekday_interval" class="form-control" value="7"/>'))))
                            .append($('<div class="row"></div>')
                                .append($('<div class="col" id="add_weekday_alert"></div>')))
                            .append($('<div class="row"></div>')
                                .append($('<div class="col"></div>')
                                    .append($('<button id="add_weekday_ok" class="btn btn-secondary btn-block">添加<i class="fa fa-check"></i></button>'))))
                        ));
        };
        var setProperty = function (info) {
            select_list.html("");

            if(info!==null&&info!==undefined) {
                maxCount = info["maxCount"];
                currentCount = info["values"].length;
                for(var i = 0; i < currentCount; ++i) {
                    var value = info.values[i];
                    if(value instanceof Date) value = fmt_dt_date(value, "yyyy-mm-dd hh:ii");
                    else value = fmt_dt_json(value, "yyyy-mm-dd hh:ii");
                    select_list.append($('<option></option>').text(value));
                }
                add_weekday_count.val(maxCount - currentCount);
            }else{
                maxCount = 0;
                currentCount = 0;
            }
            setAddEnabled();
        };
        var getValues = function () {
            var ret = [];
            var selected = select_list.find("option");
            for(var i = 0; i < selected.length; ++i) {
                ret.push(selected[i].text);
            }
            return ret;
        };
        var setAddEnabled = function () {
            //根据目前的current和max，设置add按钮的可用性，并重新初始化tab状态。
            add_new_button.attr("disabled", currentCount >= maxCount);
            add_weekday_button.attr("disabled", currentCount >= maxCount);
            add_tab.hide();
        };
        var obj = buildHtml();

        var maxCount = 0;
        var currentCount = 0;

        var add_weekday_date_cache = null; //暂时缓存weekday中的日期。

        var select_list = obj.find("#select_list");
        var add_new_button = obj.find("#add_new_button");
        var add_weekday_button = obj.find("#add_weekday_button");
        var trash_item_button = obj.find("#trash_item_button");
        var add_tab = obj.find("#add_tab");
        var add_new_tab = obj.find("#add_new_tab");
        var add_weekday_tab = obj.find("#add_weekday_tab");
        var add_new_picker = obj.find("#add_new_picker");
        var add_weekday_picker = obj.find("#add_weekday_picker");
        var add_weekday_count = obj.find("#add_weekday_count");
        var add_weekday_interval = obj.find("#add_weekday_interval");
        var add_weekday_alert = obj.find("#add_weekday_alert");
        var add_weekday_ok = obj.find("#add_weekday_ok");


        add_new_picker.datetimepicker({
            format: "yyyy-MM-dd hh:ii",
            autoclose: true,
            todayBtn: true,
            pickerPosition: "bottom-left"
        }).on('changeDate', function (event) {
            select_list.append($('<option></option>').text(fmt_dt_date(event.date, "yyyy-mm-dd hh:ii")));
            currentCount += 1;
            setAddEnabled();
        });
        add_weekday_picker.datetimepicker({
            format: "yyyy-MM-dd hh:ii",
            autoclose: true,
            todayBtn: true,
            pickerPosition: "bottom-left"
        }).on('changeDate', function (event) {
            add_weekday_date_cache = event.date;
        });
        add_weekday_ok.click(function () {
            //提交一组日期。但在这之前需要检查错误。
            var error = null;
            //1. cache是否存在
            if(add_weekday_date_cache === null) error = "请先选择起始日期。";
            //2. 总数量是否超出限制
            var sum = parseInt(add_weekday_count.val());
            if(isNaN(sum))error = "请输入合法的总数量数字。";
            else if(sum > maxCount - currentCount) error = "数量不建议超过剩余数量限制("+(maxCount - currentCount)+")。";
            else if(sum <= 0) error = "总数量需要是正数。";
            //3. 间隔天数是否合法
            var interval = parseInt(add_weekday_interval.val());
            if(isNaN(interval))error = "请输入合法的间隔天数。";
            else if(interval <= 0) error = "间隔天数需要是正数。";
            //执行
            if(error !== null){
                add_weekday_alert.append($('<div class="alert alert-warning alert-dismissable">')
                    .append($('<button type="button" class="close" data-dismiss="alert">&times;</button>'))
                    .append(error));
            }else{
                var nowDate = add_weekday_date_cache;
                for(var i = 0; i < sum; ++i) {
                    select_list.append($('<option></option>').text(fmt_dt_date(nowDate, "yyyy-mm-dd hh:ii")));
                    nowDate.setDate(nowDate.getDate() + interval);
                }
                currentCount += sum;
                setAddEnabled();
            }
        });
        add_new_button.click(function () {
            add_tab.show();
            add_new_tab.show();
            add_weekday_tab.hide();
        });
        add_weekday_button.click(function () {
            add_tab.show();
            add_weekday_tab.show();
            add_new_tab.hide();
        });
        trash_item_button.click(function () {
            //删除。
            var set = select_list.find("option:selected");
            var num = set.length;
            set.remove();
            currentCount -= num;
            trash_item_button.attr("disabled", true);
            setAddEnabled();
        });
        select_list.change(function () {
            if(select_list.find("option:selected").length > 0) {
                trash_item_button.attr("disabled", false);
            }
        });
        setProperty(info);
        return {
            obj: obj,
            setProperty: setProperty,
            getValues: getValues
        }
    },
    /** 具有下拉菜单选择特性的按钮。本质上类似于一个select。
     * @param info 参数。
     * {
     *      select: [{name: <显示名称>, value: <触发事件时返回的数据>}, title*: <在标题上的显示名称>] 展示的内容
     *      icon*: <标题上的显示图标>
     *      defaultValue: <默认展示的数据> 不写时默认展示第一个。
     *      change: function(value) 改变内容时，触发事件.使用set修改内容不会触发事件。
     * }
     */
    dropdownButton: function (info) {
        var buildHtml = function () {
            var menu = $('<div class="dropdown-menu"></div>');
            if("select" in info) {
                for(var i in info.select) {
                    var item = info.select[i];
                    var menuB = $('<button class="btn-link dropdown-item"></button>').text(item.name).click(getFuncSelect(i));
                    menu.append(menuB);
                }
            }
            var title = $('<button id="title_button" type="button" class="btn btn-secondary " data-toggle="dropdown"></button>');
            return $('<div class="btn-group"></div>')
                .append(title)
                .append(menu);
        };
        var getFuncSelect = function (index) {
            return function () {
                currentIndex = index;
                title_button.html("");
                title_button.append(info.select[currentIndex].title?info.select[currentIndex].title:info.select[currentIndex].name);
                if("icon" in info) title_button.append($('<i class="ml-1 fa fa-' + info.icon + '"></i>'));
                if("change" in info && $.isFunction(info.change)) {
                    info.change(info.select[currentIndex].value);
                }
            }
        };


        var obj = buildHtml();

        var title_button = obj.find("#title_button");

        var currentIndex = 0;
        if("defaultValue" in info && "select" in info) {
            for(var i in info.select) if(info.select[i].value === info.defaultValue) {
                currentIndex = i;
                break;
            }
        }
        title_button.append(info.select[currentIndex].title?info.select[currentIndex].title:info.select[currentIndex].name);
        if("icon" in info) title_button.append($('<i class="ml-1 fa fa-' + info.icon + '"></i>'));

        return {
            obj: obj
        }
    },
    /**构造一个模态框。
     * info: {
     *      title: string
     *      content: html-string,
     *      button_content: html-string,
     *      button_class: string,
     *      delegate: function()
     * }
     * @returns {{obj: *}}
     */
    modalDialog: function(){
        var buildHtml = function () {
            return $('<div class="modal fade"></div>')
                .append($('<div class="modal-dialog"></div>')
                    .append($('<div class="modal-content"></div>')
                        .append($('<div class="modal-header"></div>')
                            .append($('<h4 class="modal-title">Header</h4>'))
                            .append($('<button type="button" class="close" data-dismiss="modal">&times;</button>')))
                        .append($('<div class="modal-body"></div>'))
                        .append($('<div class="modal-footer"></div>')
                            .append($('<button id="check_button" type="button" class="btn" data-dismiss="modal">确认</button>')))));

        };
        var openDialog = function (info) {
            var title = ("title" in info)?info.title:"";
            var content = ("content" in info)?info.content:"";
            var buttonContent = ("button_content" in info)?info.button_content:"";
            var buttonClass = ("button_class" in info)?info.button_class:"";
            delegate = ("delegate" in info)?info.delegate:null;
            header.text(title);
            body.html("");
            body.append(content);
            checkButton.attr("class", "btn " + buttonClass);
            checkButton.html("");
            checkButton.append(buttonContent);
            obj.modal('show');
        };

        var obj = buildHtml();
        var checkButton = obj.find("#check_button");
        var header = obj.find(".modal-title");
        var body = obj.find(".modal-body");
        
        checkButton.click(function () {
            if($.isFunction(delegate))delegate();
        });

        var delegate = null;

        return {
            obj: obj,
            openDialog: openDialog
        }
    },
    weekdayTable: function (rest) {
        var buildHtml = function () {
            return $('<div class="row">')
                .append($('<div class="col" id="weekday_content">'))
        };
        var addAlert = function (content) {
          obj.find("#weekday_content")
              .append($('<div class="alert alert-danger alert-dismissable">')
              .append($('<button type="button" class="close" data-dismiss="alert">&times;</button>'))
              .append(content));
        };
        var buildTable = function(data) {
            var tb = obj.find("#weekday_content");
            var tbTitle = [undefined, 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
            var tbColor = [undefined, 'primary', 'success', 'info', 'warning', 'danger', 'secondary', 'dark'];
            for(var i = 1; i <= 7; ++i) {
                tb.append('<hr>')
                    .append($('<div class="row">')
                        .append($('<div class="col-2 col-lg-1">')
                            .append('<span class="badge badge-' + tbColor[i] + '">' + tbTitle[i] + '</span>'))
                        .append($('<div class="col">')
                            .append($('<div class="row" id="weekday_seq_' + i + '">')))
                    );
            }
            var weekFirst = get_weekday_first(new Date());
            var cardList = [undefined, [], [], [], [], [], [], []];
            for(i in data) {
                var item = data[i];
                //在至少有1个plan，且该plan的时间是本周/下周时，可以加入列表。
                //根据plan[0]的getDay,划分到不同的行。
                if(item.publish_plan.length > 0 && item.total_episode > item.publish_episode) {
                    var itsDate = new Date(item.publish_plan[0]);
                    var itsWeek = get_weekday_first(itsDate);
                    var minus = week_minus(itsWeek, weekFirst);
                    if(minus >= 0 && minus <= 1) {
                        var weekday = itsDate.getDay();
                        if(weekday === 0)weekday = 7;
                        cardList[weekday].push({
                            name: item.name,
                            minus: minus,
                            date: itsDate,
                            hour: itsDate.getHours(),
                            minute: itsDate.getMinutes(),
                            next: item.publish_episode + 1
                        });
                    }
                }
            }
            //排序，并添加card
            for(i = 1; i <= 7; ++i) {
                var list = cardList[i];
                var seq = obj.find("#weekday_seq_" + i);
                list.sort(function (a, b) {
                    return (a.minus !== b.minus)?((a.minus < b.minus)?-1:1):
                        (a.hour !== b.hour)?((a.hour < b.hour)?-1:1):
                            (a.minute !== b.minute)?((a.minute < b.minute)?-1:1):0;
                });
                for(var index in list) {
                    item = list[index];
                    seq.append($('<div class="card pt-2 pb-2 pl-3 pr-3 mr-2">')
                        //.append($('<div class="card-body">')
                            .append('<h5><strong>' + item.name + '</strong></h5>')
                            .append('<small><span class="badge badge-' + tbColor[i] + '">' + ((item.minus===1)?'下周':'') + fmt_dt_date(item.date, "hh:ii") + ' 第' + item.next + '话</span></small>')
                        //)
                    )
                }
            }

        };
        var obj = buildHtml();
        if(rest) {
            rest.request(null, function (success, status, data) {
                if(success) {
                    buildTable(data.content);
                }else{
                    if("message" in data) addAlert(status + ": " + data.message);
                    else addAlert("错误 " + status);
                }
            });
        }else{
            buildTable([]);
        }




        return obj;
    }
};