using GMap.NET;
using GMap.NET.MapProviders;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;

namespace NavControlLibrary.Map
{
    public class MapModel : NotifyModel
    {
        protected GMapProvider mProvider;
        protected PointLatLng mPoint;
        protected double? mAccuracy = null;
        protected double? mSpeed = null;
        protected double? mSpeedAccuracy = null;
        protected double? mBearing = null;
        protected double? mBearingAccuracy = null;
        protected DateTime mTime;


        string mRouteFileName = "";

        #region Свойства
        [JsonProperty("route")]
        public string RouteFileName
        {
            get { return mRouteFileName; }
            set
            {
                mRouteFileName = value;
                NotifyPropertyChanged("RouteFileName");
            }
        }
        [JsonProperty("provider")]
        public GMapProvider Provider
        {
            get { return mProvider; }
            set
            {
                mProvider = value;
                NotifyPropertyChanged("Provider");
            }
        }
        [JsonProperty("point")]
        public PointLatLng Point
        {
            get { return mPoint; }
            set
            {
                mPoint = value;
                NotifyPropertyChanged("Point");
            }
        }
        [JsonIgnore]
        public string Speed
        {
            get
            {
                if (mSpeed != null)
                {
                    return ((double)mSpeed).ToString("#0") + " км/ч" + " " + mBearing?.ToString("#0") + "°";
                }
                else
                {
                    return "";
                }
            }
        }
        [JsonIgnore]
        public string Accuracy
        {
            get
            {
                string str = "";
                if (mAccuracy == null) str = "?м";
                else str = ((double)mAccuracy).ToString("F1") + "м";
                if (mSpeedAccuracy == null) str += ", ?";
                else str += ", " + ((double)mSpeedAccuracy).ToString("F0") + "км/ч";
                if (mBearingAccuracy == null) str += ", ?°";
                else str += ", " + ((double)mBearingAccuracy).ToString("F0") + "°";
                return str;
            }
        }
        [JsonIgnore]
        public double Bearing
        {
            get
            {
                if (mBearing == null) return 0.0;
                return (double)mBearing;
            }
            set
            {
                if (mBearing != value)
                {
                    mBearing = value;
                    NotifyPropertyChanged("Bearing");
                }
            }
        }
        #endregion Свойства


        public MapModel()
        {
            mProvider = GMapProviders.GoogleMap;
            mPoint = new PointLatLng(55.4799, 37.3193);
            mTime = DateTime.Now;
        }

        public void SetFromJSON(string str)
        {
            JObject gps = JObject.Parse(str);
            if (gps["time"] != null)
            {
                //                mTime = new DateTime((string)gps["time"]);
            }

            if ((gps["position"]["latitude"] != null) && (gps["position"]["longitude"] != null))
            {
                Point = new PointLatLng((double)gps["position"]["latitude"], (double)gps["position"]["longitude"]);
            }
            if (gps["position"]["accuracy"] != null)
            {
                mAccuracy = (double)gps["position"]["accuracy"];
            }
            else
            {
                mAccuracy = null;
            }

            if ((gps["speed"] != null) && (gps["speed"]["value"] != null))
            {
                mSpeed = (double)gps["speed"]["value"];
                NotifyPropertyChanged("Speed");
                NotifyPropertyChanged("SpeedScale");
                if (gps["speed"]["accuracy"] != null) mSpeedAccuracy = (double)gps["speed"]["accuracy"];
                else mSpeedAccuracy = null;
            }
            else
            {
                mSpeedAccuracy = null;
                if (mSpeed != null)
                {
                    mSpeed = null;
                    NotifyPropertyChanged("Speed");
                }
            }

            if ((gps["bearing"] != null) && (gps["bearing"]["value"] != null))
            {
                Bearing = (double)gps["bearing"]["value"];
                if (gps["bearing"]["accuracy"] != null) mBearingAccuracy = (double)gps["bearing"]["accuracy"];
                else mBearingAccuracy = null;
            }
            else
            {
                mBearing = null;
                mBearingAccuracy = null;
            }
            NotifyPropertyChanged("Accuracy");
        }
    }
}
