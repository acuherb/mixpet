/**
 * 供应商
 */
export interface Supplier {
    supplierId?: number;
    supplierName: string;
    contact?: string | null;
    phone?: string | null;
    address?: string | null;
    note?: string | null;
    active?: boolean | null;
}

export const exampleSupplier = (): Supplier => ({
    supplierName: '', contact: '', phone: '', address: '', note: '', active: true
})
