package com.querystringframework.core;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
* @author reinaldo.locatelli
 */
public class QueryString {

    private String select;
    private String filterAnd;
    private String filterOr;
    private String filter;
    private String order;
    private String from;
    private String limit;
    private String offset;
    private String group;
    private String join;

    public QueryString(String query) {

        if (query != null) {
            try {
                stringProcess(query);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(QueryString.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void stringProcess(String query) throws NoSuchFieldException {
        boolean ignoreProcess = false;

        Field[] fields = this.getClass().getDeclaredFields();

        int start = 0;
        if (query != null && query.length() > 3) {
            Field field = null;
            for (int i = 0; i < query.length() ; i++) {

                if (query.substring(i, i + 1).toLowerCase().equals("\"")) {
                    ignoreProcess = !ignoreProcess;
                    continue;
                }
                if (ignoreProcess) {
                    continue;
                }

                for (Field f : fields) {
                    if (i + f.getName().length() + 1 <= query.length()) {;
                        if (query.substring(i, i + f.getName().length() + 1).equals(f.getName() + ":")) {
                            setAtributeValue(field, query, start, i);
                            start = i + f.getName().length() + 1;
                            field = f;
                            i += f.getName().length();
                            continue;
                        }
                    }
                }

            }

            setAtributeValue(field, query, start, query.length());
        }
    }

    public void setAtributeValue(Field field, String query, int start, int end) {
        if (field != null) {
            try {
                String value = query.substring(start, end);
                if (value != null && !value.isEmpty() && value.substring(value.length() - 1, value.length()).equals(";")) {
                    value = value.substring(0, value.length() - 1);
                }
                field.set(this, value);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(QueryString.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(QueryString.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public QueryString() {
    }

    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public String getFilterAnd() {
        return filterAnd;
    }

    public void setFilterAnd(String filterAnd) {
        this.filterAnd = filterAnd;
    }

    public String getFilterOr() {
        return filterOr;
    }

    public void setFilterOr(String filterOr) {
        this.filterOr = filterOr;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getJoin() {
        return join;
    }

    public void setJoin(String join) {
        this.join = join;
    }
    
}
