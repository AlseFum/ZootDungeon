# 可露希尔地牢
ZOOT！
明日方舟主题的地牢改版。
这个改版只用于测试开发，并不保证游戏体验。

## IdeaBucket
### EventBus
以事件类型作为区分对象
每个事件都有默认的bus线
Event.of(arg1,arg2,arg3);
是创建事件，但不分发。
Event.of(...).dispatch();
是触发事件，允许中断？
Event.of(...).collect();
是触发事件，并收集结果。结果要求使用result模式。
有优先级的。

然后是typed和any类型
any类型应当事先做出规定

不过仍然可以做出多条bus线。
## 贡献
本项目仍依循GPL-3.0协议，修改部分参见git历史
受限于时间精力，这个项目并不能稳定更新，仍欢迎PR和issue，也同样欢迎对此仓库的再次修改分发
（建discord服务器的话会有人来吗，会吗）