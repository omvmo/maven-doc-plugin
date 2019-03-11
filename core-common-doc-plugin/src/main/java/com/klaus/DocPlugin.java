package com.klaus;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * @phase package
 * @goal doc
 */
public class DocPlugin extends AbstractMojo {

    /**
     * @parameter property="${classPath}"
     */
    private String classPath;
    /**
     * @parameter property="${libDir}"
     */
    private String libDir;
    /**
     * @parameter property="${basePackage}"
     */
    private String basePackage;
    /**
     * @parameter property="${targetFile}"
     */
    private String targetFile;
    /**
     * @parameter property="${host}"
     */
    private String host;

    URLClassLoader loader=null;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.getLog().info("classPath : " + classPath);
        this.getLog().info("libDir : " + libDir);
        this.getLog().info("basePackage : " + basePackage);
        this.getLog().info("targetFile : " + targetFile);
        this.getLog().info("host : " + host);
        URLStreamHandler us = null;
        try {
            //初始化类加载器
            String basePath = (new URL("file",null,new File(classPath).getCanonicalPath() + File.separator)).toString();
            String libPath = (new URL("file",null,new File(libDir).getCanonicalPath() + File.separator)).toString();

            URLStreamHandler sh = null;
            List<URL> libs = new ArrayList<URL>();
            File libDir = new File(libPath.replaceAll("file:", ""));
            //找到所有的lib目录下的依赖
            for (File jar : libDir.listFiles()) {
                libs.add(new URL(null,libPath + jar.getName(),sh));
            }

            this.getLog().info(""+libs.size());
            libs.add(new URL(null,basePath,sh));
            this.getLog().info(""+libs.size());
            //加载所有的依赖和项目
            loader = new URLClassLoader(libs.toArray(new URL[libs.size()]),Thread.currentThread().getContextClassLoader());

            List<Class<?>> classes= new ArrayList<>();
            File classDir = new File(classPath);
            //找到classes目录下的类
            list(classes,classDir);

            if(classes.size() == 0){
                this.getLog().info("未加载任何类文件");
                return;
            }
            this.getLog().info(classes.size()+"");
            //解析
            HTMLParser.generate(targetFile, classes, host);
            this.getLog().info("已成功生成文档");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 递归扫描所有的class文件
     *
     * @param clazz
     * @param dir
     */
    private void list(List<Class<?>> clazz, File dir) {
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                list(clazz, f);
            } else {
                if (!f.getName().endsWith(".class")) { continue; }
                String className = f.getPath().replaceAll("\\\\", "/").replaceAll(classPath.replaceAll("\\\\", "/"), "").replaceAll("/", ".").replaceAll("\\.class", "");
                className = className.substring(1, className.length());
                if (className.startsWith(basePackage)) {
                    try {
                        clazz.add(Class.forName(className, true, loader));
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        }
    }
}
