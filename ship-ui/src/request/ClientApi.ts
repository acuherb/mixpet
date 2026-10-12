import {YClientList} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Client} from "@/model/entiy/Client";
import {NameDo} from "@/model/DO/NameDo";
import {Res} from "@/request/Res";

// 后台用户列表
export const reqClientList = (page: YClientList): Promise<Page<Client>> =>
    reqApi<string, Page<Client>>("client/page", {params: page})
// 后台下拉框查询用户姓名
export const reqClientName = (): Promise<NameDo[]> =>
    reqApi<string, NameDo[]>("client")
// 管理员新增用户
export const reqAddClient = (data: Client): Promise<Res> =>
    reqApi.post<string, Res>("client", data)
// 管理员修改用户
export const reqUpdateClient = (data: Client): Promise<Res> =>
    reqApi.put<string, Res>("client", data)
// 管理员删除用户
export const reqDelClient = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("client/" + id)
// 管理员批量删除用户
export const reqDelClientBatch = (ids: number[]): Promise<Res> =>
    reqApi.delete<string, Res>("client/batch/" + ids)
// 订单下拉框查询有宠物的用户姓名，带上已有用户
export const reqClientNameHavePet = (clientId: number | null): Promise<NameDo[]> => {
    if (clientId == null || clientId == 0) return reqApi<string, NameDo[]>("client/full")
    else return reqApi<string, NameDo[]>("client/full?clientId=" + clientId)
}
// 用户修改自己的资料
export const reqUpdateClientSelf = (data: Client): Promise<Res> =>
    reqApi.put<string, Res>("client/self", data)

// 用户修改自己的登录密码
export const reqUpdatePwd = (data: { oldPwd: string; newPwd: string }): Promise<Res> =>
    reqApi.put<string, Res>("client/password", data)
// 管理员重置某用户密码为 123456
export const reqResetPwd = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("client/reset/" + id)
// 用户绑定/修改邮箱
export const reqUpdateEmail = (data: { email: string; code: string }): Promise<Res> =>
    reqApi.put<string, Res>("client/email", data)
// 检查客户名下关联
export const reqCheckClientRef = (id: number): Promise<Res> =>
    reqApi<string, Res>("client/ref/" + id)
// 停用客户
export const reqDeactivateClient = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("client/deactivate/" + id, {})
// 启用客户
export const reqActivateClient = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("client/activate/" + id, {})
// 前台为客户充值（存入余额）
export const reqRechargeClient = (clientId: number, money: number): Promise<Res> =>
    reqApi.put<string, Res>("client/recharge", {}, {params: {clientId, money}})
