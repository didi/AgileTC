package com.xiaoju.framework.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by didi on 2019/9/29.
 */
public class FileUtil {
    /**
     * 解压文件
     * @param zipPath 要解压的目标文件
     * @param descDir 指定解压目录
     * @return 解压结果：成功，失败
     */
    @SuppressWarnings("rawtypes")
    public static boolean decompressZip(String zipPath, String descDir) {

        File zipFile = new File(zipPath);
        boolean flag = false;
        File pathFile = new File(descDir);
        if(!pathFile.exists()){
            pathFile.mkdirs();
        }
        try {
            ZipFile zip = new ZipFile(zipFile, Charset.forName("gbk"));
            for(Enumeration entries = zip.entries(); entries.hasMoreElements();){
                ZipEntry entry = (ZipEntry)entries.nextElement();
                String zipEntryName = entry.getName();
                InputStream in = zip.getInputStream(entry);
                //指定解压后的文件夹+当前zip文件的名称
                String outPath = (descDir+zipEntryName).replace("/", File.separator);
                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf(File.separator)));
                if(!file.exists()){
                    file.mkdirs();
                }
                //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if(new File(outPath).isDirectory()){
                    continue;
                }
                //保存文件路径信息（可利用md5.zip名称的唯一性，来判断是否已经解压）
                System.err.println("当前zip解压之后的路径为：" + outPath);
                OutputStream out = new FileOutputStream(outPath);
                byte[] buf1 = new byte[2048];
                int len;
                while((len=in.read(buf1))>0){
                    out.write(buf1,0,len);
                }
                in.close();
                out.close();
            }
            flag = true;
            //必须关闭，要不然这个zip文件一直被占用着，要删删不掉，改名也不可以，移动也不行，整多了，系统还崩了。
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 压缩文件
     * @param sourcePath 要压缩的文件夹
     * @param destPath 压缩文件放的地方
     * @return 压缩文件
     */
    public static String compressZip(String sourcePath, String destPath) {
        File resourcesFile = new File(sourcePath);
        File targetFile = new File(destPath);
        //如果目的路径不存在，则新建
        if(!targetFile.exists()){
            targetFile.mkdirs();
        }

        String targetName = "mm"+".xmind";
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(destPath+"\\"+targetName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(outputStream));

        try {
            createCompressedFile(out, resourcesFile, "");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void createCompressedFile(ZipOutputStream out,File file,String dir) throws Exception{
        //如果当前的是文件夹，则进行进一步处理
        if(file.isDirectory()){
            //得到文件列表信息
            File[] files = file.listFiles();
            //将文件夹添加到下一级打包目录
            out.putNextEntry(new ZipEntry(dir+"/"));

            dir = dir.length() == 0 ? "" : dir +"/";

            //循环将文件夹中的文件打包
            for(int i = 0 ; i < files.length ; i++){
                createCompressedFile(out, files[i], dir + files[i].getName());
            }
        }
        else{   //当前的是文件，打包处理
            //文件输入流
            FileInputStream fis = new FileInputStream(file);

            out.putNextEntry(new ZipEntry(dir));
            //进行写操作
            int j =  0;
            byte[] buffer = new byte[1024];
            while((j = fis.read(buffer)) > 0){
                out.write(buffer,0,j);
            }
            //关闭输入流
            fis.close();
        }
    }



    /** 删除文件夹*/
    public static void delete(File file) {

        if(!file.exists()) return;

        if(file.isFile() || file.list()==null) {
            file.delete();
            System.out.println("删除了"+file.getName());
        }else {
            File[] files = file.listFiles();
            for(File a:files) {
                delete(a);
            }
            file.delete();
            System.out.println("删除了"+file.getName());
        }
    }

    /** 读取解析json文件*/
    public static String readJsonFile(String filePath) {
        String jsonStr = "";
        String fileName = "content.json";
        String jsonFile = (filePath+fileName).replace("/", File.separator);
        try {
            File file = new File(jsonFile);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(file), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
