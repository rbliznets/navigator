using Newtonsoft.Json.Linq;
using System;

namespace NavControlLibrary.Models
{
    public class GPSTriggerModel : NotifyModel
    {
        static double lng = 0.0;
        static double ltd = 0.0;

        #region Поля
        double mLongitude = lng;
        string tLongitude = "";
        double mLatitude = ltd;
        string tLatitude = "";
        double mRadius = 25.0;
        double mPrior = 0.0;
        double mPost = 0.0;
        double? mBearing = null;
        int mDelay = 0;
        string tDelay = "";

        static bool unlockMap = false;
        #endregion Поля

        #region Свойства
        public bool IsMapUnlock
        {
            get => unlockMap;
            set
            {
                unlockMap = value;
                NotifyPropertyChanged(nameof(unlockMap));
            }
        }
        public int Delay
        {
            get
            {
                return mDelay;
            }
            set
            {
                if (value < 0) value = 0;
                else if (value > 600) value = 600;
                mDelay = value;
                NotifyPropertyChanged(nameof(Delay));
                NotifyPropertyChanged(nameof(TextDelay));
            }
        }
        public string TextDelay
        {
            get
            {
                if (GetErrors("TextDelay") == null)
                {
                    if (Delay == 0) return "Выключена";
                    else return Delay.ToString() + " сек";
                }
                else return tDelay;
            }
            set
            {
                tDelay = value;
                if (string.IsNullOrWhiteSpace(value))
                {
                    SetError("TextDelay", "Должно быть число.");
                }
                else
                {
                    if (value.IndexOf(' ') >= 0) value = value.Substring(0, value.IndexOf(' '));
                    if (value == "Выключена")
                    {
                        ClearError("TextDelay");
                        Delay = 0;
                    }
                    else if (int.TryParse(value, out int x))
                    {
                        if (x < 0) SetError("TextDelay", "Должно быть не меньше 0.");
                        else
                        {
                            ClearError("TextDelay");
                            Delay = x;
                        }
                    }
                    else
                    {
                        SetError("TextDelay", "Нужно численное значение.");
                    }
                }
            }

        }
        public bool IsBearing
        {
            get
            {
                return Bearing != null;
            }
            set
            {
                if (value && (Bearing == null)) Bearing = 0;
                else if ((!value) && (Bearing != null)) Bearing = null;
            }
        }
        public double? Bearing
        {
            get
            {
                return mBearing;
            }
            set
            {
                if (value != null)
                {
                    if (value < 0) value = 0;
                    else if (value > 359) value = 359;
                }
                mBearing = value;
                NotifyPropertyChanged(nameof(Bearing));
                NotifyPropertyChanged(nameof(IsBearing));
            }
        }
        public bool IsPost
        {
            get
            {
                return (Post != Radius) && (Post != 0.0);
            }
        }
        public double Post
        {
            get
            {
                return mPost;
            }
            set
            {
                if (value > 150) value = 150;
                else if (value < Radius) value = Radius;
                mPost = value;
                NotifyPropertyChanged(nameof(Post));
                NotifyPropertyChanged(nameof(IsPost));
            }
        }
        public bool IsPrior
        {
            get
            {
                return (Prior != Radius) && (Prior != 0.0);
            }
        }
        public double Prior
        {
            get
            {
                return mPrior;
            }
            set
            {
                if (value > 150) value = 150;
                else if (value < Radius) value = Radius;
                mPrior = value;
                NotifyPropertyChanged(nameof(Prior));
                NotifyPropertyChanged(nameof(IsPrior));
            }
        }
        public double Radius
        {
            get
            {
                return mRadius;
            }
            set
            {
                if (value > 100) value = 100;
                else if (value < 20) value = 20;
                mRadius = value;
                if (Prior < value) Prior = value;
                if (Post < value) Post = value;
                NotifyPropertyChanged(nameof(Radius));
                NotifyPropertyChanged(nameof(IsPrior));
                NotifyPropertyChanged(nameof(IsPost));
            }
        }
        public double Latitude
        {
            get
            {
                return mLatitude;
            }
            set
            {
                if (value < -90) value = -90;
                else if (value > 90) value = 90;
                mLatitude = value;
                ltd = value;
                NotifyPropertyChanged(nameof(Latitude));
                NotifyPropertyChanged(nameof(TextLatitude));
            }
        }
        public string TextLatitude
        {
            get
            {
                if (GetErrors("TextLatitude") == null)
                {
                    return Latitude.ToString("F5") + "°";
                }
                else return tLatitude;
            }
            set
            {
                tLatitude = value;
                if (string.IsNullOrWhiteSpace(value))
                {
                    SetError("TextLatitude", "Должно быть число.");
                }
                else
                {
                    if (value.IndexOf(' ') >= 0) value = value.Substring(0, value.IndexOf(' '));
                    if (value.IndexOf('°') >= 0) value = value.Substring(0, value.IndexOf('°'));
                    if (double.TryParse(value, out double x))
                    {
                        ClearError("TextLatitude");
                        Latitude = x;
                    }
                    else
                    {
                        SetError("TextLatitude", "Нужно численное значение.");
                    }
                }
            }
        }
        public double Longitude
        {
            get
            {
                return mLongitude;
            }
            set
            {
                if (value < -180) value = -180;
                else if (value > 180) value = 180;
                mLongitude = value;
                lng = value;
                NotifyPropertyChanged("Longitude");
                NotifyPropertyChanged("TextLongitude");
            }
        }
        public string TextLongitude
        {
            get
            {
                if (GetErrors("TextLongitude") == null)
                {
                    return Longitude.ToString("F5") + "°";
                }
                else return tLongitude;
            }
            set
            {
                tLongitude = value;
                if (string.IsNullOrWhiteSpace(value))
                {
                    SetError("TextLongitude", "Должно быть число.");
                }
                else
                {
                    if (value.IndexOf(' ') >= 0) value = value.Substring(0, value.IndexOf(' '));
                    if (value.IndexOf('°') >= 0) value = value.Substring(0, value.IndexOf('°'));
                    if (double.TryParse(value, out double x))
                    {
                        ClearError("TextLongitude");
                        Longitude = x;
                    }
                    else
                    {
                        SetError("TextLongitude", "Нужно численное значение.");
                    }
                }
            }
        }
        #endregion Свойства

        public GPSTriggerModel(JToken json, DIR_TYPES? dir)
        {
            if (json["lon"] != null) Longitude = (double)json["lon"];
            if (json["lat"] != null) Latitude = (double)json["lat"];
            if (json["radius"] != null) Radius = (double)json["radius"];
            if (json["prior"] != null) Prior = (double)json["prior"];
            if (json["post"] != null) Post = (double)json["post"];
            if (json["bearing"] != null) Bearing = (double)json["bearing"];
            if (dir == DIR_TYPES.BACKWARD)
            {
                if (Bearing < 180) Bearing += 180;
                else Bearing -= 180;
            }
            if (json["delay"] != null) Delay = (int)json["delay"];
        }

        internal JToken GetJToken(DIR_TYPES? dir)
        {
            JObject res = new JObject();
            res["lon"] = Longitude;
            res["lat"] = Latitude;
            if (Radius != 25) res["radius"] = Radius;
            if ((Prior != 0) && (Prior != Radius)) res["prior"] = Prior;
            if ((Post != 0) && (Post != Radius)) res["post"] = Post;
            if (IsBearing)
            {
                if (dir == DIR_TYPES.BACKWARD)
                {
                    double b;
                    if (Bearing < 180) b = (double)Bearing + 180;
                    else b = (double)Bearing - 180;
                    res["bearing"] = b;
                }
                else
                {
                    res["bearing"] = Bearing;
                }
            }
            if (Delay != 0) res["delay"] = Delay;
            return res;
        }

        public void InitLast()
        {
            lng = Longitude;
            ltd = Latitude;
        }

        public GPSTriggerModel()
        {
        }

        public double CalculateDistance(GPSTriggerModel point)
        {
            var d1 = Latitude * (Math.PI / 180.0);
            var num1 = Longitude * (Math.PI / 180.0);
            var d2 = point.Latitude * (Math.PI / 180.0);
            var num2 = point.Longitude * (Math.PI / 180.0) - num1;
            var d3 = Math.Pow(Math.Sin((d2 - d1) / 2.0), 2.0) +
                     Math.Cos(d1) * Math.Cos(d2) * Math.Pow(Math.Sin(num2 / 2.0), 2.0);
            return 6376500.0 * (2.0 * Math.Atan2(Math.Sqrt(d3), Math.Sqrt(1.0 - d3)));
        }

        public JObject ToJson()
        {
            JObject res = new JObject();
            res["lon"] = Longitude;
            res["lat"] = Latitude;
            return res;
        }


        public override string ToString()
        {
            string str = "gps{" + Latitude.ToString() + ";" + Longitude.ToString();
            if (IsPrior) str += ";<" + Prior.ToString("#");
            str += ";r" + Radius.ToString("#");
            if (IsPost) str += ";" + Post.ToString("#") + ">";
            if (IsBearing) str += ";b" + Bearing?.ToString("#");
            if (Delay != 0.0) str += ";" + Delay.ToString() + "sec";
            str += "}";
            return str;
        }
    }
}
