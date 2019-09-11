package utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.JBColor;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.awt.*;
import java.util.Locale;

public class Util {
    // 通过strings.xml获取的值
    private static String stringValue;

    /**
     * 显示dialog
     *
     * @param editor
     * @param result 内容
     * @param time   显示时间，单位秒
     */
    public static void showPopupBalloon(final Editor editor, final String result, final int time) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                JBPopupFactory factory = JBPopupFactory.getInstance();
                factory.createHtmlTextBalloonBuilder(result, null, new JBColor(new Color(116, 214, 238), new Color(76, 112, 117)), null)
                        .setFadeoutTime(time * 1000)
                        .createBalloon()
                        .show(factory.guessBestPopupLocation(editor), Balloon.Position.below);

            }
        });
    }

    /**
     * 驼峰  app_text转换成appText
     *
     * @param fieldName
     * @return
     */
    public static String getFieldName(String fieldName) {
        if (!TextUtils.isEmpty(fieldName)) {
            String[] names = fieldName.split("_");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                sb.append(firstToUpperCase(names[i]));
            }
            fieldName = sb.toString();
        }
        return fieldName;
    }

    /**
     * 第一个字母大写
     *
     * @param key
     * @return
     */
    public static String firstToUpperCase(String key) {
        return key.substring(0, 1).toUpperCase(Locale.CHINA) + key.substring(1);
    }


    /**
     * 解析xml核心代码
     * 获取所有id
     *
     * @param file
     * @param list
     * @return
     */
    public static java.util.List<Element> getIDsFromLayout(final PsiFile file, final java.util.List<Element> list) {
        file.accept(new XmlRecursiveElementVisitor() {
            /**
             * 系统会解析xml里面的文件有多少发控件，visitElement方法的执行几次
             * @param element
             */
            @Override
            public void visitElement(PsiElement element) {

                super.visitElement(element);

                //判断节点元素是xml标签
                if (element instanceof XmlTag) {
                    XmlTag xmlTag = (XmlTag) element;
                    //获取标签名-->TextView
                    String name = xmlTag.getName();
                    //如果name是include就需要另外处理--->获取布局名称，解析这布局
                    //equalsIgnoreCase忽略大小写的比较
                    if (name.equalsIgnoreCase("include")) {
                        //include信息--->layout="@layout/activity_text"
                        XmlAttribute layout = xmlTag.getAttribute("layout", null);
                        //   <include layout="@layout/activity_text"/>
                        //获取布局名称  layout.getValue()==@layout/activity_text
                        //getLayoutName()方法返回activity_text
                        String layoutName = getLayoutName(layout.getValue());

                        //获取project对象
                        Project project = file.getProject();
                        //获取布局文件--》例如activity_main.xml文件
                        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, layoutName + ".xml", GlobalSearchScope.allScope(project));

                        //定义个xml标签对象
                        XmlFile include = null;
                        if (psiFiles.length > 0) {
                            include = (XmlFile) psiFiles[0];
                        }

                        if (include != null) {
                            //递归解析
                            getIDsFromLayout(include, list);
                            return;
                        }


                    }

                    /**
                     * 获取元素信息，保存到自定义的Element对象里面
                     */
                    //id信息
                    //xmlTag.getAttribute这个方法参数就相当布局key 返回对应value
                    //例如 android:id="@+id/text" --》android:id就key  返回
                    XmlAttribute value = xmlTag.getAttribute("android:id");

                    //如果控件没有id就不做处理
                    if (value == null) {
                        return;
                    }

                    //反回值 --》@+id/text
                    String id = value.getValue();
                    if (TextUtils.isEmpty(id)) {
                        return;
                    }

                    //获取控件类型 比如 TextVie Butter
                    XmlAttribute aClass = xmlTag.getAttribute("class");
                    if (aClass != null) {
                        //获取全类名--》包名+TextView
                        name = aClass.getValue();
                    }

                    //保存到封装类Element里面
                    Element element1 = new Element(name, id, xmlTag);
                    //保存到集合
                    list.add(element1);


                }


            }
        });


        return list;
    }

    /**
     * layout.getValue()返回的值为@layout/layout_view
     * 该方法返回layout_view
     *
     * @param layout
     * @return
     */
    public static String getLayoutName(String layout) {
        if (layout == null || !layout.startsWith("@") || !layout.contains("/")) {
            return null;
        }
        // @layout layout_view
        String[] parts = layout.split("/");
        if (parts.length != 2) {
            return null;
        }
        // layout_view
        return parts[1];
    }

    /**
     * 根据当前文件获取对应的class文件
     *
     * @param editor
     * @param file
     * @return
     */
    public static PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        } else {
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            return target instanceof SyntheticElement ? null : target;
        }
    }


}
