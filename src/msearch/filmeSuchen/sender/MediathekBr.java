/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package msearch.filmeSuchen.sender;

import msearch.filmeSuchen.MSearchFilmeSuchen;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import msearch.io.MSearchGetUrl;
import msearch.daten.MSearchConfig;
import msearch.daten.DatenFilm;
import msearch.tool.GuiFunktionen;
import msearch.tool.MSearchConst;
import msearch.tool.MSearchLog;
import msearch.tool.MSearchStringBuilder;

public class MediathekBr extends MediathekReader implements Runnable {

    public static final String SENDER = "BR";
    private SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);         // "date": "2013-07-06 13:00:00", 

    public MediathekBr(MSearchFilmeSuchen ssearch, int startPrio) {
        super(ssearch, /* name */ SENDER, /* threads */ 2, /* urlWarten */ 500, startPrio);
    }

    @Override
    void addToList() {
        //new Thread(new ThemaLaden()).start();
        meldungStart();
        Thread th;
        th = new Thread(new ThemaLaden());
        th.setName(nameSenderMReader);
        th.start();
        if (MSearchConfig.senderAllesLaden) {
            th = new Thread(new ArchivLaden(1, 100));
            th.setName(nameSenderMReader);
            th.start();
            th = new Thread(new ArchivLaden(101, 200));
            th.setName(nameSenderMReader);
            th.start();
            th = new Thread(new ArchivLaden(201, 300));
            th.setName(nameSenderMReader);
            th.start();
            th = new Thread(new ArchivLaden(301, 400));
            th.setName(nameSenderMReader);
            th.start();
        }
    }

    private class ThemaLaden implements Runnable {

        @Override
        public synchronized void run() {
            meldungAddMax(1);
            meldungAddThread();
            try {
                jsonSuchen();
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-203069877, MSearchLog.FEHLER_ART_MREADER, "MediathekBr.JsonLaden.run", ex, "");
            }
            meldungThreadUndFertig();
        }
    }

    private class ArchivLaden implements Runnable {

        int anfang, ende;

        public ArchivLaden(int aanfang, int eende) {
            anfang = aanfang;
            ende = eende;
        }

        @Override
        public synchronized void run() {
            meldungAddMax(ende - anfang);
            meldungAddThread();
            try {
                archivSuchen(anfang, ende);
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-203069877, MSearchLog.FEHLER_ART_MREADER, "MediathekBr.JsonLaden.run", ex, "");
            }
            meldungThreadUndFertig();
        }
    }

    private void jsonSuchen() {
        //"date": "2013-07-13 13:00:00", 
        //"description": "Pflanzentricks - Wie erreicht eine Pflanze, was sie will?", 
        //"duration": "25", 
        //"image": "http://mediathek-video.br.de/listra/sendungsbilder/xenius_xl.jpg", 
        //"title": "X:enius", 
        //"videos": {
        //"l": {
        //  "bitrate": 700000, 
        //  "url": "http://hbbtv.b7.br.gl-systemhaus.de/b7/konks/1373715065-b7konks_nc_203101728_107154.mp4"
        //}, 
        //"m": {
        //  "bitrate": 200000, 
        //  "url": "http://hbbtv.b7.br.gl-systemhaus.de/b7/konks/1373715063-b7konks_nc_203101728_107153.mp4"
        //}, 
        //"xl": {
        //  "bitrate": 2000000, 
        //  "url": "http://hbbtv.b7.br.gl-systemhaus.de/b7/konks/1373715066-b7konks_nc_203101728_107155.mp4"
        //}
        final String URL_JSON = "http://rd.gl-systemhaus.de/br/b7/nc/jsonp/latestarchive.json";
        final String DATE = "\"date\": \"";
        final String DESCRIPTION = "\"description\": \"";
        final String DURATION = "\"duration\": \"";
        final String IMAGE = "\"image\": \"";
        final String THEMA = "\"title\": \"";
        final String URL__ = "\"xl\":";
        final String URL = "\"url\": \"";
        final String URL_KLEIN__ = "\"l\":";
        final String URL_KLEIN = "\"url\": \"";
        String date, datum, zeit, thema, titel, description, duration, image, url, url_klein;
        long dauer;
        MSearchStringBuilder seite1 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        MSearchGetUrl getUrl = new MSearchGetUrl(wartenSeiteLaden);
        seite1 = getUrl.getUri(nameSenderMReader, URL_JSON, MSearchConst.KODIERUNG_UTF, 3/*max Versuche*/, seite1, URL_JSON);
        if (seite1.length() == 0) {
            MSearchLog.fehlerMeldung(-302590789, MSearchLog.FEHLER_ART_MREADER, "MediathekBr.jsonSuchen", "Leere Seite: " + URL_JSON);
            return;
        }
        int pos, posStop;
        int pos1;
        int pos2;
        pos = 0;
        while (!MSearchConfig.getStop() && (pos = seite1.indexOf(DATE, pos)) != -1) {
            date = "";
            datum = "";
            zeit = "";
            thema = "";
            titel = "";
            description = "";
            duration = "";
            image = "";
            url = "";
            url_klein = "";
            dauer = 0;
            try {
                pos += DATE.length();
                posStop = seite1.indexOf(DATE, pos);
                pos1 = pos;
                if ((pos2 = seite1.indexOf("\"", pos1)) != -1) {
                    date = seite1.substring(pos1, pos2);
                    datum = convertDatumJson(date);
                    zeit = convertZeitJson(date);
                }
                description = seite1.extract(DESCRIPTION, "\"", pos, posStop);
                final String TRENNER = " - ";
                if (description.contains(TRENNER)) {
                    titel = description.substring(0, description.indexOf(TRENNER)).trim();
                    description = description.substring(description.indexOf(TRENNER) + TRENNER.length()).trim();
                } else if (description.length() > 25) {
                    titel = description.substring(0, 25).trim() + "...";
                } else {
                    titel = description;
                    description = "";
                }
                duration = seite1.extract(DURATION, "\"", pos, posStop);
                if (!duration.equals("")) {
                    try {
                        dauer = Long.parseLong(duration) * 60;
                    } catch (Exception ex) {
                        MSearchLog.fehlerMeldung(-304973047, MSearchLog.FEHLER_ART_MREADER, "MediathekBR.jsonSuchen", ex, "duration: " + duration);
                    }
                }
                thema = seite1.extract(THEMA, "\"", pos, posStop);
                image = seite1.extract(IMAGE, "\"", pos, posStop);
                url = seite1.extract(URL__, URL, "\"", pos, posStop);
                url_klein = seite1.extract(URL_KLEIN__, URL_KLEIN, "\"", pos, posStop);
                if (url.isEmpty() & !url_klein.isEmpty()) {
                    url = url_klein;
                    url_klein = "";
                } else if (url.isEmpty()) {
                    continue;
                }
                thema = GuiFunktionen.utf8(thema);
                titel = GuiFunktionen.utf8(titel);
                description = GuiFunktionen.utf8(description);
                // public DatenFilm(String ssender, String tthema, String filmWebsite, String ttitel, String uurl, String datum, String zeit,
                //   long duration, String description, String thumbnailUrl, String imageUrl, String[] keywords) {
                DatenFilm film = new DatenFilm(nameSenderMReader, thema, "http://www.br.de/mediathek/index.html", titel, url, ""/*rtmpURL*/, datum, zeit,
                        dauer, description, image, new String[]{});
                film.addUrlKlein(url_klein, "");
                addFilm(film);
                meldung(film.arr[DatenFilm.FILM_URL_NR]);
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-902483073, MSearchLog.FEHLER_ART_MREADER, "MediathekBR.jsonSuchen", ex, "");
            }
        }
    }

    private void archivSuchen(int start, int ende) {
        // http://www.br.de/service/suche/archiv102.html?documentTypes=video&page=1&sort=date
        final String MUSTER_ADRESSE_1 = "http://www.br.de/service/suche/archiv102.html?documentTypes=video&page=";
        final String MUSTER_ADRESSE_2 = "&sort=date";
        final String MUSTER_START = "<div class=\"teaser search_result\">";
        MSearchStringBuilder seiteArchiv1 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        MSearchStringBuilder seiteArchiv2 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        MSearchGetUrl getUrl = new MSearchGetUrl(wartenSeiteLaden);
        for (int i = start; i <= ende; ++i) {
            if (MSearchConfig.getStop()) {
                break;
            }
            String adresse = MUSTER_ADRESSE_1 + i + MUSTER_ADRESSE_2;
            meldungProgress(adresse);
            seiteArchiv1 = getUrl.getUri(nameSenderMReader, adresse, MSearchConst.KODIERUNG_UTF, 2 /* versuche */, seiteArchiv1, "" /* Meldung */);
            if (seiteArchiv1.length() == 0) {
                MSearchLog.fehlerMeldung(-912036478, MSearchLog.FEHLER_ART_MREADER, MediathekBr.class.getName() + ".addToList_addr", "Leere Seite für URL: " + adresse);
            }
            int pos = 0, stop = 0;
            String url, titel, thema, datum, beschreibung;
            while (!MSearchConfig.getStop() && (pos = seiteArchiv1.indexOf(MUSTER_START, pos)) != -1) {
                pos += MUSTER_START.length();
                stop = seiteArchiv1.indexOf(MUSTER_START, pos);
                url = seiteArchiv1.extract("<a href=\"", "\"", pos, stop);
                thema = seiteArchiv1.extract("teaser_overline\">", "<", pos, stop).trim();
                if (thema.endsWith(":")) {
                    thema = thema.substring(0, thema.lastIndexOf(":"));
                }
                titel = seiteArchiv1.extract("teaser_title\">", "<", pos, stop);
                // <p class="search_date">23.08.2013 | BR-alpha</p>
                datum = seiteArchiv1.extract("search_date\">", "<", pos, stop);
                if (datum.contains("|")) {
                    datum = datum.substring(0, datum.indexOf("|")).trim();
                }
                beschreibung = seiteArchiv1.extract("<p>", "<", pos, stop);
                if (url.equals("")) {
                    MSearchLog.fehlerMeldung(-636987451, MSearchLog.FEHLER_ART_MREADER, MediathekBr.class.getName() + ".addToList_addr", "keine URL: " + adresse);
                } else {
                    url = "http://www.br.de" + url;
                    archivAdd1(getUrl, seiteArchiv2, url, thema, titel, datum, beschreibung);
                }
            }
        }
    }

    private void archivAdd1(MSearchGetUrl getUrl, MSearchStringBuilder seiteArchiv2, String urlThema, String thema, String titel, String datum, String beschreibung) {
        // http://www.br.de/service/suche/archiv102.html?documentTypes=video&page=1&sort=date
        meldung(urlThema);
        seiteArchiv2 = getUrl.getUri(nameSenderMReader, urlThema, MSearchConst.KODIERUNG_UTF, 1 /* versuche */, seiteArchiv2, "" /* Meldung */);
        if (seiteArchiv2.length() == 0) {
            MSearchLog.fehlerMeldung(-912036478, MSearchLog.FEHLER_ART_MREADER, MediathekBr.class.getName() + ".addToList_addr", "Leere Seite für URL: " + urlThema);
        }
        String url, urlFilm = "", urlFilmKlein = "", groesse = "", duration = "", bild = "";
        long dauer = 0;
        url = seiteArchiv2.extract("setup({dataURL:'", "'");
        bild = seiteArchiv2.extract("setup({dataURL:'", "\" src=\"", "\"");
        if (!bild.isEmpty()) {
            bild = "http://www.br.de" + bild;
        }
        if (url.equals("")) {
            MSearchLog.fehlerMeldung(-834215987, MSearchLog.FEHLER_ART_MREADER, MediathekBr.class.getName() + ".archivAdd1", "keine URL: " + urlThema);
        } else {
            url = "http://www.br.de" + url;
            seiteArchiv2 = getUrl.getUri(nameSenderMReader, url, MSearchConst.KODIERUNG_UTF, 1 /* versuche */, seiteArchiv2, "" /* Meldung */);
            if (seiteArchiv2.length() == 0) {
                MSearchLog.fehlerMeldung(-397123654, MSearchLog.FEHLER_ART_MREADER, MediathekBr.class.getName() + ".addToList_addr", "Leere Seite für URL: " + urlThema);
            }
            // <asset type="STANDARD">
            int start;
            if ((start = seiteArchiv2.indexOf("<asset type=\"STANDARD\">")) != -1) {
                urlFilmKlein = seiteArchiv2.extract("<serverPrefix>", "<", start) + seiteArchiv2.extract("<fileName>", "<", start);
                // <readableSize>281 MB</readableSize>
                groesse = seiteArchiv2.extract("<readableSize>", "<", start);
            }
            if ((start = seiteArchiv2.indexOf("<asset type=\"PREMIUM\">")) != -1) {
                urlFilm = seiteArchiv2.extract("<serverPrefix>", "<", start) + seiteArchiv2.extract("<fileName>", "<", start);
                if (!urlFilm.isEmpty()) {
                    groesse = seiteArchiv2.extract("<readableSize>", "<", start);
                }
            }
            if (groesse.contains("MB")) {
                groesse = groesse.replace("MB", "").trim();
            }
            // <duration>00:44:15</duration>
            duration = seiteArchiv2.extract("<duration>", "<");
            if (!duration.equals("")) {
                try {
                    String[] parts = duration.split(":");
                    long power = 1;
                    for (int i = parts.length - 1; i >= 0; i--) {
                        dauer += Long.parseLong(parts[i]) * power;
                        power *= 60;
                    }
                } catch (Exception ex) {
                    MSearchLog.fehlerMeldung(-304973047, MSearchLog.FEHLER_ART_MREADER, "MediathekBR.jsonSuchen", ex, "duration: " + duration);
                }
            }
            if (urlFilm.isEmpty()) {
                urlFilm = urlFilmKlein;
            }
            if (urlFilm.equals("")) {
                MSearchLog.fehlerMeldung(-978451236, MSearchLog.FEHLER_ART_MREADER, MediathekBr.class.getName() + ".archivAdd1", "keine URL: " + urlThema);
            } else if (dauer == 0 || dauer > 600) {
                // nur anlegen, wenn länger als 10 Minuten, sonst nur Schnipsel
                //public DatenFilm(String ssender, String tthema, String filmWebsite, String ttitel, String uurl, String uurlRtmp,
                //String datum, String zeit, long dauerSekunden, String description, String thumbnailUrl, String imageUrl, String[] keywords) {
                DatenFilm film = new DatenFilm(nameSenderMReader, thema, urlThema, titel, urlFilm, "",
                        datum, "", dauer, beschreibung, bild, new String[]{});
                if (!urlFilmKlein.isEmpty()) {
                    film.addUrlKlein(urlFilmKlein, "");
                }
                try {
                    Integer.parseInt(groesse);
                    film.arr[DatenFilm.FILM_GROESSE_NR] = groesse;
                } catch (Exception ex) {
                    // dann wars nix
                }
                addFilm(film);

            }
        }
    }

    private String convertDatumJson(String datum) {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").format(sdfIn.parse(datum));
        } catch (Exception ex) {
            MSearchLog.fehlerMeldung(-963297249, MSearchLog.FEHLER_ART_MREADER, "MediathekBR.convertDatum", ex);
        }
        return "";
    }

    private String convertZeitJson(String datum) {
        try {
            return new SimpleDateFormat("HH:mm:ss").format(sdfIn.parse(datum));
        } catch (Exception ex) {
            MSearchLog.fehlerMeldung(-963297249, MSearchLog.FEHLER_ART_MREADER, "MediathekBR.convertDatum", ex);
        }
        return "";
    }

    public String convertDatum(String datum) {
        //      <beginnPlan>2010-12-09T10:55:00</beginnPlan>
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date filmDate = sdfIn.parse(datum);
            SimpleDateFormat sdfOut;
            sdfOut = new SimpleDateFormat("dd.MM.yyyy");
            datum = sdfOut.format(filmDate);
        } catch (Exception ex) {
            MSearchLog.fehlerMeldung(-210365944, MSearchLog.FEHLER_ART_MREADER, "MediathekBr.convertDatum", ex, "");
        }
        return datum;
    }

    public String convertTime(String datum) {
        //      <beginnPlan>2010-12-09T10:55:00</beginnPlan>
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date filmDate = sdfIn.parse(datum);
            SimpleDateFormat sdfOut;
            sdfOut = new SimpleDateFormat("HH:mm:ss");
            datum = sdfOut.format(filmDate);
        } catch (Exception ex) {
            MSearchLog.fehlerMeldung(-573690176, MSearchLog.FEHLER_ART_MREADER, "MediatheBr.convertTime", ex, "");
        }
        return datum;
    }
}