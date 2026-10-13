/**
 * 影像检查项目字典
 */
export interface ImageTest {
    itemId: number;
    itemName: string;
    groupName: string;
    price: number;
    detail: string;
}

export const exampleImageTest = (): ImageTest => ({
    itemId: 0,
    itemName: '',
    groupName: '',
    price: 0,
    detail: ''
})

export const imageGroups = ['X光', 'B超', 'CT']