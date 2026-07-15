package hkid;

public class HkidTest {
    public static void main(String[] args) {
        HkidNum hkidNum = HkidNumUtil.genRandomHkidNum();
        System.out.println("HKID Number: " + hkidNum);
        System.out.println("HKID Number: " + hkidNum.toString(HkidNum.Format.WithoutParentheses));
        System.out.println("HKID Number: " + hkidNum.toString(HkidNum.Format.Complete));

        HKID hkid = HKIDUtil.genRandomHkid();
        System.out.println("HKID: " + hkid);
        System.out.println("HKID Number: " + hkid.getHkidNum());
        System.out.println("getChiName: " + hkid.getChiName());
        System.out.println("getChiNameInfo: " + hkid.getChiNameInfo());
        System.out.println("getChiFullName: " + hkid.getChiFullName());
        System.out.println("getChiCommercialCode: " + hkid.getChiCommercialCode());
        System.out.println("getEngName: " + hkid.getEngName());
        System.out.println("getEngNameInfo: " + hkid.getEngNameInfo());
        System.out.println("getEngFullName: " + hkid.getEngFullName());
        System.out.println("getSex: " + hkid.getSex());
        System.out.println("getSexCode: " + hkid.getSexCode());
        System.out.println("getDateOfBirth: " + hkid.getDateOfBirth());
        System.out.println("getAge: " + hkid.getAge());

        try{
            System.out.println("hkid: " + new HkidNum("A123456(7)"));
        } catch (IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
    }
}
