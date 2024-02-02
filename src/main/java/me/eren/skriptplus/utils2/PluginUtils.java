package me.eren.skriptplus.utils2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PluginUtils {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy---HH:mm");
    private static final String GITHUB_API = "https://api.github.com/repos/%s/releases/latest";

    /**
     * @return the formatted current date
     */
    public static String getCurrentDate() {
        Date currentDate = new Date();
        return DATE_FORMAT.format(currentDate);
    }

    public static String getGitHubAPI(String organization, String repo) {
        return String.format(GITHUB_API, organization + "/" + repo);
    }

    /**
     * @param clazz the class of the method
     * @param method the method
     * @param params the parameters to invoke
     * @return the returned object, if any
     * @throws RuntimeException if reflection fails
     */
    public static Object executeMethod(Class<?> clazz, String method, Object[] params) throws RuntimeException {
        try {
            Method m = clazz.getDeclaredMethod(method);
            m.setAccessible(true);
            return m.invoke(params);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error while running '" + method + "' from '" + clazz + "'.", e);
        }
    }

}
