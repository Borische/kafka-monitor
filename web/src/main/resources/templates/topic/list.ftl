<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <#include "../common/layui.ftl">
</head>
<body>

<div class="layui-fluid">
    <div class="layui-card">

        <div class="layui-form layui-card-header layuiadmin-card-header-auto">
            <div class="layui-form-item">
                <div class="layui-inline">主题名称</div>
                <div class="layui-inline" style="width:200px">
                    <input type="text" name="topicName" placeholder="请输入主题名称" autocomplete="off" class="layui-input">
                </div>
                <div class="layui-inline">
                    <button class="layui-btn layuiadmin-btn-admin" lay-submit lay-filter="search">
                        <i class="layui-icon layui-icon-search layuiadmin-button-btn"></i>
                    </button>
                </div>
            </div>
        </div>


        <div class="layui-card-body">
            <table id="grid" lay-filter="grid"></table>
            <script type="text/html" id="grid-toolbar">
                <div class="layui-btn-container">
                    <button class="layui-btn layui-btn-sm layuiadmin-btn-admin" lay-event="add">添加</button>
                </div>
            </script>

            <script type="text/html" id="grid-bar">
                <a class="layui-btn layui-btn-normal layui-btn-xs" lay-event="edit"><i
                            class="layui-icon layui-icon-edit"></i>编辑</a>
                <a class="layui-btn layui-btn-danger layui-btn-xs" lay-event="del"><i
                            class="layui-icon layui-icon-delete"></i>删除</a>
            </script>
        </div>
    </div>
</div>

<script>
    layui.config({base: '../../../layuiadmin/'}).extend({index: 'lib/index'}).use(['index', 'table'], function () {
        var admin = layui.admin, form = layui.form, table = layui.table;
        form.on('submit(search)', function (data) {
            var field = data.field;
            table.reload('grid', {where: field});
        });
        table.render({
            elem: '#grid',
            url: 'list',
            toolbar: '#grid-toolbar',
            method: 'post',
            //height: 'full-100',
            cellMinWidth: 80,
            page: true,
            limit: 15,
            limits: [15],
            even: true,
            text: "对不起，加载出现异常！",
            cols: [[
                {type: 'numbers', title: '序号', width: 50},
                {field: 'name', title: '主题名称'},
                {field: 'partitionNum', title: '分区数', width: 100},
                {field: 'partitionIndex', title: '分区索引', width: 100},
                {field: 'createTime', title: '创建时间', width: 200},
                {field: 'modifyTime', title: '修改时间', width: 200},
                {fixed: 'right', title: '操作', toolbar: '#grid-bar', width: 150}
            ]]
        });


        table.on('toolbar(grid)', function (obj) {
            var checkStatus = table.checkStatus(obj.config.id);
            switch (obj.event) {
                case 'batchDel':
                    var data = checkStatus.data;
                    if (data.length > 0) {
                        layer.confirm(admin.DEL_QUESTION, function (index) {
                            var keys = "";
                            for (var j = 0, len = data.length; j < len; j++) {
                                keys = keys + data[j].id + ","
                            }
                            admin.post("del", {ids: keys}, function () {
                                table.reload('grid');
                                layer.close(index);
                            });
                        });
                    } else {
                        admin.error(admin.SYSTEM_PROMPT, admin.DEL_ERROR);
                    }
                    break;
                case 'add':
                    layer.open({
                        type: 2,
                        title: admin.ADD,
                        content: 'toadd.html',
                        area: ['880px', '350px'],
                        btn: admin.BUTTONS,
                        resize: false,
                        yes: function (index, layero) {
                            var iframeWindow = window['layui-layer-iframe' + index], submitID = 'btn_confirm',
                                submit = layero.find('iframe').contents().find('#' + submitID);
                            iframeWindow.layui.form.on('submit(' + submitID + ')', function (data) {
                                var field = data.field;
                                admin.post('add', field, function () {
                                    table.reload('grid');
                                    layer.close(index);
                                }, function (result) {
                                    admin.error(admin.OPT_FAILURE, result.error);
                                    layer.close(index);
                                });
                            });
                            submit.trigger('click');
                        }
                    });
                    break;
            }
            ;
        });

        table.on('tool(grid)', function (obj) {
            var data = obj.data;
            if (obj.event === 'del') {
                layer.confirm(admin.DEL_QUESTION, function (index) {
                    admin.post("del", {ids: data.id}, function () {
                        table.reload('grid');
                        layer.close(index);
                    });
                });
            } else if (obj.event = 'edit') {
                layer.open({
                    type: 2,
                    title: admin.EDIT,
                    content: 'toedit.html?id=' + data.id,
                    area: ['880px', '400px'],
                    btn: admin.BUTTONS,
                    resize: false,
                    yes: function (index, layero) {
                        var iframeWindow = window['layui-layer-iframe' + index], submitID = 'btn_confirm',
                            submit = layero.find('iframe').contents().find('#' + submitID);
                        iframeWindow.layui.form.on('submit(' + submitID + ')', function (data) {
                            var field = data.field;
                            admin.post('edit', admin.toJson(field), function () {
                                table.reload('grid');
                                layer.close(index);
                            }, function (result) {
                                admin.error(admin.OPT_FAILURE, result.error);
                                layer.close(index);
                            });
                        });
                        submit.trigger('click');
                    }
                });
            }
        });
    });
</script>


</body>
</html>