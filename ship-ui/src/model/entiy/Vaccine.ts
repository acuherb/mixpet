/**
 * 疫苗字典
 */
export interface Vaccine {
    vaccineId: number;
    vaccineName: string;
    brand: string | null;
    prevent: string | null;
    intervalDays: number;
    price: number;
}

export const exampleVaccine = (): Vaccine => ({
    vaccineId: 0,
    vaccineName: '',
    brand: '',
    prevent: '',
    intervalDays: 365,
    price: 0
})