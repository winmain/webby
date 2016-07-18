package webby.commons.time;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple wrapper around {@link SimpleDateFormat}. Makes it possible to get correctly ended month
 * names in dates.
 * <p>
 * <b>Внимание!</b> Класс не является thread-safe, т.к. родительский SimpleDateFormat не
 * thread-safe.
 */
@NotThreadSafe
public class RussianDateFormat extends SimpleDateFormat {

    private static final String[] monthI = { "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь" };
    private static final String[] monthR = { "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря" };

    public RussianDateFormat(String format) {
        super(format, new Locale("ru"));
    }

    private String formatEndings(String str) {
        return StringUtils.replaceEach(str, monthI, monthR);
    }

    private String unformatEndings(String str) {
        return StringUtils.replaceEach(str, monthR, monthI);
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        return new StringBuffer(formatEndings(super.format(date, toAppendTo, pos).toString()));
    }

    public Date parse(String text, ParsePosition pos) {
        text = unformatEndings(text);
        return super.parse(text, pos);
    }
}
