/**
 * 医院职务/职称字典
 */
export interface Job {
    jobId: number;
    jobName: string;
    sort?: number;
    createTime?: string | null;
    updateTime?: string | null;
}

export const exampleJob = (): Job => ({
    jobId: 0,
    jobName: '',
    sort: 99
})
