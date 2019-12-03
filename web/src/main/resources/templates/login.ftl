<@compress single_line=true>
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
        <#include "common/layui.ftl">
        <link rel="stylesheet" href="${ctx}/layuiadmin/style/login.css" media="all">
    </head>
    <body>

    <div class="layadmin-user-login layadmin-user-display-show" id="LAY-user-login" style="display: none;">

        <div class="layadmin-user-login-main">
            <div class="layadmin-user-login-box layadmin-user-login-header">
                <h2>Kafka管理监控平台</h2>
                <p>made by pegasus.</p>
            </div>
            <div class="layadmin-user-login-box layadmin-user-login-body layui-form">
                <div class="layui-form-item">
                    <label class="layadmin-user-login-icon layui-icon layui-icon-username" for="username"></label>
                    <input type="text" name="username" id="username" lay-verify="required" placeholder="请输入用户名"
                           class="layui-input" autofocus="autofocus">
                </div>
                <div class="layui-form-item">
                    <label class="layadmin-user-login-icon layui-icon layui-icon-password" for="password"></label>
                    <input type="password" name="password" id="password" lay-verify="required" placeholder="请输入密码"
                           class="layui-input">
                </div>
                <div class="layui-form-item" style="margin-bottom: 20px;">

                </div>
                <div class="layui-form-item">
                    <button id="btnSubmit" name="btnSubmit" class="layui-btn layui-btn-fluid" lay-submit
                            lay-filter="btnSubmit">登 入
                    </button>
                </div>
            </div>
        </div>
    </div>

    <script src="${ctx}/layuiadmin/layui/layui.js"></script>
    <script>
        layui.config({base: '../../layuiadmin/'}).extend({index: 'lib/index'}).use(['index', 'user'], function () {
            const $ = layui.$, admin = layui.admin, form = layui.form;
            form.render();
            form.on('submit(btnSubmit)', function (obj) {
                admin.post('login', obj.field, function () {
                    layer.msg('登入成功', {
                        offset: '15px'
                        , icon: 1
                        , time: 1000
                    }, function () {
                        location.href = '../index';
                    });
                });
            });

            $(document).keydown(function (event) {
                if (event.keyCode === 13) {
                    $("#btnSubmit").trigger("click");
                    return false
                }
            });
        });
    </script>
    </body>
    </html>
</@compress>