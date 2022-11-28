using Newtonsoft.Json.Linq;
using System;

namespace NavControlLibrary.Models
{
    public class GPSData
    {
        public DateTime Time;
        public double Latitude;
        public double Longitude;

        public double? Accuracy = null;
        public double Speed = 0.0;
        public double? SpeedAccuracy = null;
        public double? Bearing = null;
        public double? BearingAccuracy = null;
        public double? Altitude = null;
        public double? AltitudeAccuracy = null;

        public bool Fifo = false;

        private double deg2rad(double deg)
        {
            return (deg * Math.PI / 180.0);
        }

        private double rad2deg(double rad)
        {
            return (rad / Math.PI * 180.0);
        }
        private double distance(double lat1, double lon1, double lat2, double lon2)
        {
            if ((lat1 == lat2) && (lon1 == lon2))
            {
                return 0;
            }
            else
            {
                double theta = lon1 - lon2;
                double dist = Math.Sin(deg2rad(lat1)) * Math.Sin(deg2rad(lat2)) + Math.Cos(deg2rad(lat1)) * Math.Cos(deg2rad(lat2)) * Math.Cos(deg2rad(theta));
                dist = Math.Acos(dist);
                dist = rad2deg(dist);
                dist = dist * 60 * 1.1515;
                dist = dist * 1609.344;
                return (dist);
            }
        }

        public double getDistanceTo(GPSData dt)
        {
            return distance(Latitude, Longitude, dt.Latitude, dt.Longitude);
        }

        private double bearing(double lt1, double ln1, double lt2, double ln2)
        {
            var lat1 = deg2rad(lt1);
            var lat2 = deg2rad(lt2);
            var long1 = deg2rad(ln2);
            var long2 = deg2rad(ln1);
            var dLon = long1 - long2;

            var y = Math.Sin(dLon) * Math.Cos(lat2);
            var x = Math.Cos(lat1) * Math.Sin(lat2) - Math.Sin(lat1) * Math.Cos(lat2) * Math.Cos(dLon);
            var brng = Math.Atan2(y, x);

            return (rad2deg(brng) + 360) % 360;
        }


        public double getDeltaBearing(double a1)
        {
            if (Bearing == null) return 0.0;
            double r1;
            double r2;
            if (a1 > Bearing)
            {
                r1 = a1 - (double)Bearing;
                r2 = (double)Bearing - a1 + 360;
            }
            else
            {
                r1 = (double)Bearing - a1;
                r2 = a1 - (double)Bearing + 360;
            }
            if (r1 > r2) r1 = r2;
            return r1;
        }


        public double getDeltaTime(GPSData dt)
        {
            TimeSpan span = Time.Subtract(dt.Time);
            return span.TotalSeconds;
        }

        public double getBearing(GPSData dt)
        {
            return bearing(Latitude, Longitude, dt.Latitude, dt.Longitude);
        }

        public GPSData(string json)
        {
            JObject gps = JObject.Parse(json);
            if (gps["time"] != null)
            {
                Time = DateTime.Parse((string)gps["time"]);
            }

            if ((gps["position"]["latitude"] != null) && (gps["position"]["longitude"] != null))
            {
                Latitude = (double)gps["position"]["latitude"];
                Longitude = (double)gps["position"]["longitude"];
            }
            if (gps["position"]["accuracy"] != null)
            {
                Accuracy = (double)gps["position"]["accuracy"];
            }

            if ((gps["speed"] != null) && (gps["speed"]["value"] != null))
            {
                Speed = (double)gps["speed"]["value"];
                if (gps["speed"]["accuracy"] != null) SpeedAccuracy = (double)gps["speed"]["accuracy"];
            }

            if ((gps["bearing"] != null) && (gps["bearing"]["value"] != null))
            {
                Bearing = (double)gps["bearing"]["value"];
                if (gps["bearing"]["accuracy"] != null) BearingAccuracy = (double)gps["bearing"]["accuracy"];
            }

            if ((gps["altitude"] != null) && (gps["altitude"]["value"] != null))
            {
                Altitude = (double)gps["altitude"]["value"];
                if (gps["altitude"]["accuracy"] != null) AltitudeAccuracy = (double)gps["altitude"]["accuracy"];
            }

            if (gps["fifo"] != null)
            {
                Fifo = ((int)gps["fifo"]) != 0;
            }
        }

    }
}
