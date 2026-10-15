/**
 * 电子病历模板
 */
export interface RecordTemplate {
    templateId?: number;
    name: string;
    category?: string | null;
    chiefComplaint?: string | null;
    physicalExam?: string | null;
    diagnosis?: string | null;
    treatment?: string | null;
    advice?: string | null;
    internalNote?: string | null;
    active?: boolean | null;
}

export const exampleRecordTemplate = (): RecordTemplate => ({
    name: '', category: '常见病', chiefComplaint: '', physicalExam: '',
    diagnosis: '', treatment: '', advice: '', internalNote: '', active: true
})
