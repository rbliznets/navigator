using Microsoft.WindowsAPICodePack.Shell;
using Microsoft.WindowsAPICodePack.Shell.PropertySystem;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;

namespace NavControlLibrary.Models
{
    public class VideoStepModel : NotifyModel
    {
        public delegate void eChangeGPS(VideoStepModel script);
        public event eChangeGPS onChangeGPS;

        #region Поля
        string mFullFile = "";
        DIR_TYPES mRoute = DIR_TYPES.ANY;
        GPS_LEVEL mGPS = GPS_LEVEL.NONE;

        private bool mStop = false;
        public int mListNum = -1;
        int mListSize = 0;
        public bool mPriorLast = false;
        public bool mNoneFirst = false;
        public bool mNoneLast = false;
        public bool mPostFirst = false;
        #endregion Поля

        #region Свойства
        public bool IsNotFirst
        {
            get
            {
                switch (GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        return mListNum != 1;
                    case GPS_LEVEL.NONE:
                        return !mNoneFirst;
                    default:
                        return !mPostFirst;
                }
            }
        }
        public bool IsNotLast
        {
            get
            {
                switch (GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        return !mPriorLast;
                    case GPS_LEVEL.NONE:
                        return !mNoneLast;
                    default:
                        return mListNum != mListSize;
                }
            }
        }
        public int ListSize
        {
            get
            {
                return mListSize;
            }
            set
            {
                mListSize = value;
                NotifyPropertyChanged(nameof(ListSize));
                NotifyPropertyChanged(nameof(IsNotLast));
                NotifyPropertyChanged(nameof(IsNotFirst));
            }
        }
        public bool IsStop
        {
            get
            {
                return mStop;
            }
        }
        public string Info1
        {
            get
            {
                string res = "";
                if ((!IsStop) && (Route != DIR_TYPES.ANY)) res += Route.ToString();
                if (GPS != GPS_LEVEL.NONE)
                {
                    if (res != "") res += ",";
                    res += GPS.ToString();
                }
                return res.Trim();
            }
        }
        public string Info2
        {
            get
            {
                try
                {
                    using (var shell = ShellObject.FromParsingName(FullFile))
                    {
                        IShellProperty prop1 = shell.Properties.System.Video.FrameWidth;
                        IShellProperty prop2 = shell.Properties.System.Video.FrameHeight;
                        return prop1.ValueAsObject.ToString() + "x" + prop2.ValueAsObject.ToString();
                    }
                }
                catch
                {
                    return "";
                }
            }
        }
        public string FullFile
        {
            get
            {
                return mFullFile;
            }
            set
            {
                try
                {
                    using (var shell = ShellObject.FromParsingName(value))
                    {
                        IShellProperty prop = shell.Properties.System.Media.Duration;
                        var t = (ulong)prop.ValueAsObject;
                    }

                    mFullFile = value;
                    NotifyPropertyChanged(nameof(FullFile));
                    NotifyPropertyChanged(nameof(File));
                    NotifyPropertyChanged(nameof(Duration));
                    NotifyPropertyChanged(nameof(Info2));
                }
                catch
                {

                }
            }
        }
        public double Duration
        {
            get
            {
                try
                {
                    using (var shell = ShellObject.FromParsingName(FullFile))
                    {
                        IShellProperty prop = shell.Properties.System.Media.Duration;
                        var t = (ulong)prop.ValueAsObject;
                        return TimeSpan.FromTicks((long)t).TotalSeconds;
                    }
                }
                catch
                {
                    return 0.0;
                }
            }
        }
        public string File
        {
            get
            {
                return mFullFile.Substring(mFullFile.LastIndexOf("\\") + 1);
            }
        }
        public DIR_TYPES Route
        {
            get
            {
                return mRoute;
            }
            set
            {
                mRoute = value;
                NotifyPropertyChanged(nameof(Route));
                NotifyPropertyChanged(nameof(Info1));
            }
        }
        public List<DIR_TYPES> DirTypeList
        {
            get
            {
                return Enum.GetValues<DIR_TYPES>().ToList();
            }
        }
        public GPS_LEVEL GPS
        {
            get
            {
                return mGPS;
            }
            set
            {
                mGPS = value;
                NotifyPropertyChanged(nameof(GPS));
                NotifyPropertyChanged(nameof(Info1));
                if (onChangeGPS != null) onChangeGPS(this);
            }
        }
        public List<GPS_LEVEL> GPSList
        {
            get
            {
                return Enum.GetValues<GPS_LEVEL>().ToList();
            }
        }
        #endregion Свойства

        public VideoStepModel(JToken json, string path, DIR_TYPES? route)
        {
            if (json["file"] != null) FullFile = path + (string)json["file"];
            if (json["gps"] != null)
            {
                switch ((string)json["gps"])
                {
                    case "prior":
                        GPS = GPS_LEVEL.PRIOR;
                        break;
                    case "post":
                        GPS = GPS_LEVEL.POST;
                        break;
                }
            }
            if ((route == null) || (route == DIR_TYPES.ANY))
            {
                if (json["route"] != null)
                {
                    switch ((string)json["route"])
                    {
                        case "forward":
                            Route = DIR_TYPES.FORWARD;
                            break;
                        case "backward":
                            Route = DIR_TYPES.BACKWARD;
                            break;
                    }
                }
            }
            else
            {
                Route = (DIR_TYPES)route;
                mStop = true;
            }
        }

        internal JToken GetJToken(bool isNonStop)
        {
            JObject res = new JObject();
            res["file"] = File;
            switch (GPS)
            {
                case GPS_LEVEL.PRIOR:
                    res["gps"] = "prior";
                    break;
                case GPS_LEVEL.POST:
                    res["gps"] = "post";
                    break;
            }
            if (isNonStop)
            {
                switch (Route)
                {
                    case DIR_TYPES.FORWARD:
                        res["route"] = "forward";
                        break;
                    case DIR_TYPES.BACKWARD:
                        res["route"] = "backward";
                        break;
                }
            }
            return res;
        }

        public VideoStepModel(DIR_TYPES? route)
        {
            if (route != null)
            {
                Route = (DIR_TYPES)route;
                mStop = true;
            }
        }
        public VideoStepModel(VideoStepModel step)
        {
            Route = step.Route;
            mStop = step.mStop;
            GPS = step.GPS;
        }

        public override string ToString()
        {
            string str = "{" + File;
            if (GPS != GPS_LEVEL.NONE) str += ";" + GPS.ToString();
            if (Route != DIR_TYPES.ANY) str += ";" + Route.ToString();
            str += "}";
            return str;
        }

    }
}
