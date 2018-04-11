//用于Web页面简单内容自动生成的工具集。

// 自动生成导航条的工具 ------------------------------------------------------
/**构造导航条的函数。
 * obj 传入jquery对象
 * arr 传入{title: String, link: String}的数组，表示要构造的内容。
*/

function build_navbar(obj, arr) {
    var ul = $('<ul class="nav"></ul>');
    for(var i in arr){
        var li = $('<li class="nav-item"></li>');
        var a = $('<a class="nav-link"></a>').attr('href', arr[i].link).text(arr[i].title);
        li.append(a);
        ul.append(li);
    }
    obj.append(ul);
}

/**构造侧边导航栏的函数。
 * obj 传入jquery对象
 * arr 传入{title: String, link: String}的数组，表示要构造的内容。
 */
function build_navlist(obj, arr) {
    var ul = $('<ul class="nav flex-column pt-2 pb-2">');
    for(var i in arr){
        var li = $('<li class="nav-item border border-left-0 border-right-0 m-1"></li>');
        var a = $('<a class="nav-link text-dark" href="#"></a>').attr('href', arr[i].link).text(arr[i].title);
        li.append(a);
        ul.append(li);
    }
    obj.append(ul);
}

//其他的小工具函数
/**根据参数构造GET查询URL。
 * url 基础url
 * params 参数集。
 */
function pUrl(url /*...*/) {
    var ret;
    if(url.substring(url.length-5)===".json")ret = url;
    else ret = url + ".json";
    var p_std = purl_param(arguments);
    if(p_std!==null){
        ret += "?" + p_std;
    }
    return ret;
}
function purl_param(params) {
    if(params!==null&&params.length>0){
        var ret = "";
        var first = true;
        for(k = 1; k< params.length; ++k) {
            var p = params[k];
            for(var i in p) {
                if(first)first = false;else ret += "&";
                if($.isArray(p[i])) {
                    var second = true;
                    for(var j in p[i]) {
                        if(second)second = false;else ret += "&";
                        ret += i + "=" + encodeURIComponent(p[i][j]);
                    }
                }else{
                    ret += i + "=" + encodeURIComponent(p[i]);
                }
            }
        }
        return ret;
    }else return null;
}

/**将一个从json得到的y-m-d h:min:s.ms字串格式化为指定的格式。
 */
function fmt_dt_json(json, fmt) {
    var origin = json.split(/[-:. ]/);
    var o = {
        "m+" : origin[1],                 //月份
        "d+" : origin[2],                    //日
        "h+" : origin[3],                   //小时
        "i+" : origin[4],                 //分
        "s+" : origin[5],                 //秒
        "S"  : origin[6]             //毫秒
    };
    if(/(y+)/.test(fmt)) {
        fmt=fmt.replace(RegExp.$1, (origin[0]+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(fmt)){
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
        }
    }
    return fmt;
}

/**将一个date对象转化为指定的格式。
 */
function fmt_dt_date(date, fmt) {
    var o = {
        "m+" : date.getMonth()+1,                 //月份
        "d+" : date.getDate(),                    //日
        "h+" : date.getHours(),                   //小时
        "i+" : date.getMinutes(),                 //分
        "s+" : date.getSeconds(),                 //秒
        "S"  : date.getMilliseconds()             //毫秒
    };
    if(/(y+)/.test(fmt)) {
        fmt=fmt.replace(RegExp.$1, (date.getFullYear()+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(fmt)){
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
        }
    }
    return fmt;
}
/**把datetimepicker设置视图的name转换为更直观些的数字。
 */
function datetimepicker_range_to_num(name) {
    if(name === "minute") return 0;
    else if(name=== "hour") return 1;
    else if(name === "day") return 2;
    else if(name === "month") return 3;
    else if(name === "year") return 4;
    else return 0;
}