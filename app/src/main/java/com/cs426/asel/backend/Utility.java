package com.cs426.asel.backend;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;

public final class Utility {
    public static String getUserEmail(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            return null;
        }
        return account.getEmail();
    }

    public static String parseInstant(Instant instant, String pattern) {
        return instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern(pattern));
    }

    public static Instant parseToInstant(String dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        TemporalAccessor temporalAccessor = formatter.parse(dateTime);

        // Check if the parsed date-time string contains a zone or offset
        if (temporalAccessor.query(TemporalQueries.zoneId()) == null && temporalAccessor.query(TemporalQueries.offset()) == null) {
            // If no zone or offset is found, use the system default zone
            LocalDateTime localDateTime = LocalDateTime.from(temporalAccessor);
            return localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        } else {
            // If a zone or offset is found, parse as ZonedDateTime
            ZonedDateTime zonedDateTime = ZonedDateTime.from(temporalAccessor);
            return zonedDateTime.toInstant();
        }
    }
}
