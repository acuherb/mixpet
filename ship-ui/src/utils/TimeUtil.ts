// 根据出生日期返回年龄展示字符串
// 规则：<30天返回"X天"，<365天返回"X个月"，>=365天返回"X岁"或"X岁X个月"
// 传入 null 或空字符串返回 "--"
export const getAge = (birthday: string | null | undefined, estimated = false): string => {
    if (!birthday) return '--'
    const birth = new Date(birthday)
    if (isNaN(birth.getTime())) return '--'
    const now = new Date()
    const days = Math.floor((now.getTime() - birth.getTime()) / (1000 * 3600 * 24))
    if (days < 0) return '--'
    let result: string
    if (days < 30) result = days + '天'
    else if (days < 365) result = Math.floor(days / 30) + '个月'
    else {
        const years = Math.floor(days / 365)
        const months = Math.floor((days - years * 365) / 30)
        result = months === 0 ? years + '岁' : years + '岁' + months + '个月'
    }
    return estimated ? '约 ' + result : result
}

// 去除时间中的T
export const moveT = (str: string | null | undefined): string => {
    try {
        if (str == null) return "未知"
        let formatTimeArray = str.split("T")
        formatTimeArray[1] = formatTimeArray[1].split(":")[0] + ":" + formatTimeArray[1].split(":")[1];
        return formatTimeArray.join("_");
    } catch (e) {
        return "未知"
    }
}

/**
 * 聊天页面的时间计算
 * 如果是在今天的日期，则只返回时分秒
 * 如果是今年的而不是今天，则返回月份、日期与时分
 * 如果不是今年的，则只返回年月日
 * @param datetime 输入时间 ISO 8601 格式
 */
export function getDisplayTime(datetime: string): string {
    const date = new Date(datetime);
    const now = new Date();
    if (date.getFullYear() == now.getFullYear()) {
        if (date.getMonth() == now.getMonth() && date.getDate() == now.getDate()) {
            return `${padZero(date.getHours())}:${padZero(date.getMinutes())}:${padZero(date.getSeconds())}`;
        } else {
            return `${padZero(date.getMonth() + 1)}-${padZero(date.getDate())} ${padZero(date.getHours())}:${padZero(date.getMinutes())}`;
        }
    } else {
        return `${date.getFullYear()}-${padZero(date.getMonth() + 1)}-${padZero(date.getDate())}`;
    }
}

function padZero(num: number): string {
    return num < 10 ? '0' + num : String(num);
}

/**
 * 返回输入时间到今天有多少天
 * @param dateString 输入时间，ISO 8601 格式
 */
export function getDaysFromToday(dateString: string): string {
    let today = new Date();
    let date = new Date(dateString);
    let timeDiff = Math.abs(today.getTime() - date.getTime());
    return Math.ceil(timeDiff / (1000 * 3600 * 24)) + "天";
}

/**
 * 返回当前的时间（直接生成 ISO 格式会导致时区问题）
 */
export const getNowISO = () => {
    const date = new Date();
    return date.getFullYear() + "-" + ("0" + (date.getMonth() + 1)).slice(-2)
        + "-" + ("0" + date.getDate()).slice(-2) + "T" + ("0" + date.getHours()).slice(-2)
        + ":" + ("0" + date.getMinutes()).slice(-2) + ":00"
}


// 从年龄字符串反推生日（永远返回 estimated=true，因为反推的永远是估算值）
// 支持："2岁3个月" / "2岁" / "3个月" / "45天" / "2" / "约2岁"
export const parseAgeToBirthday = (age: string): { birthday: string, estimated: boolean } | null => {
    if (!age) return null
    let s = age.trim()
    if (s.startsWith('约')) s = s.slice(1).trim()
    let years = 0, months = 0, days = 0
    const mY = s.match(/(\d+)\s*岁/)
    const mM = s.match(/(\d+)\s*(?:个月|月)/)
    const mD = s.match(/(\d+)\s*天/)
    if (mY) years = parseInt(mY[1])
    if (mM) months = parseInt(mM[1])
    if (mD) days = parseInt(mD[1])
    if (!mY && !mM && !mD) {
        const n = s.match(/^(\d+)$/)
        if (n) years = parseInt(n[1])
        else return null
    }
    const d = new Date()
    d.setFullYear(d.getFullYear() - years)
    d.setMonth(d.getMonth() - months)
    d.setDate(d.getDate() - days)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return { birthday: `${yyyy}-${mm}-${dd}`, estimated: true }
}
