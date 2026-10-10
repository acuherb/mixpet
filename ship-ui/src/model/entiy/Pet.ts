/**
 * 宠物详情
 */
export interface Pet {
    petId: number;
    petName: string;
    petVariety: string;
    petSex: boolean;
    petBirthday: string | null;
    petBirthdayEstimated: boolean;
    petStatus: string;
    petInfo: string;
    petColor: string | null;
    petWeight: number | null;
    petSterilization: boolean;
    petChip: string | null;
    petAllergy: string | null;
    petHistory: string | null;
    petAlive: boolean;
    petNote: string | null;
    clientId: number | null;
    petPhoto: string;
}

/**
 * 空数据
 */
export const examplePet = (): Pet => {
    return {
        petId: 0,
        petName: "",
        petVariety: "",
        petSex: false,
        petBirthday: null,
        petBirthdayEstimated: false,
        petStatus: "",
        petInfo: "",
        petColor: null,
        petWeight: null,
        petSterilization: false,
        petChip: null,
        petAllergy: null,
        petHistory: null,
        petAlive: true,
        petNote: null,
        clientId: null,
        petPhoto: "",
    }
}
