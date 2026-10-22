import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {LoginVo, RoleEnum} from "@/model/VO/LoginVo";
import {Client} from "@/model/entiy/Client";
import {RegisterVo} from "@/model/VO/RegisterVo";

export const reqLogin = (data: LoginVo): Promise<Res> =>
    reqApi.post<string, Res>("cao/toLogin", data)

export const reqLogout = (): Promise<Res> =>
    reqApi.post<string, Res>("cao/logout")

export const reqGetLogin = (): Promise<RoleEnum> =>
    reqApi<string, RoleEnum>("login")

export const reqLoginUser = (): Promise<Client> =>
    reqApi<string, Client>("login/user")

// 注册/绑定邮箱发送验证码（后端返回 Result，data 为验证 token）
export const reqSendRegMail = (mail: string): Promise<Res> =>
    reqApi<string, Res>("login/regMail?mail=" + mail)

export const reqRegisExist = (regUse: string): Promise<Res> =>
    reqApi<string, Res>("login/isExist?regUse=" + regUse)

export const reqRegister = (data: RegisterVo): Promise<Res> =>
    reqApi.post<string, Res>("login/register", data)

export const reqSendFindMail = (mail: string, username: string): Promise<string> =>
    reqApi<string, string>("login/findMail?mail=" + mail + "&username=" + username)

export const reqFindPwd = (data: RegisterVo): Promise<Res> =>
    reqApi.post<string, Res>("login/find", data)