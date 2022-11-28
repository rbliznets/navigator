using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;

namespace NavControlLibrary.Models
{
    public class TextStepModel : NotifyModel
    {
        public delegate void eChangeGPS(TextStepModel script);
        public event eChangeGPS onChangeGPS;

        #region Поля
        string mText = "";
        int mDelay = 15;
        string tDelay = "";
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
        public bool Display1
        {
            get
            {
                return ID.Contains(1);
            }
            set
            {
                if (value)
                {
                    if (!ID.Contains(1))
                    {
                        ID.Add(1);
                        NotifyPropertyChanged(nameof(Display1));
                    }
                }
                else
                {
                    if (ID.Contains(1))
                    {
                        ID.Remove(1);
                        NotifyPropertyChanged(nameof(Display1));
                    }
                }
            }
        }
        public bool Display2
        {
            get
            {
                return ID.Contains(2);
            }
            set
            {
                if (value)
                {
                    if (!ID.Contains(2))
                    {
                        ID.Add(2);
                        NotifyPropertyChanged(nameof(Display2));
                    }
                }
                else
                {
                    if (ID.Contains(2))
                    {
                        ID.Remove(2);
                        NotifyPropertyChanged(nameof(Display2));
                    }
                }
            }
        }
        public bool Display3
        {
            get
            {
                return ID.Contains(3);
            }
            set
            {
                if (value)
                {
                    if (!ID.Contains(3))
                    {
                        ID.Add(3);
                        NotifyPropertyChanged(nameof(Display3));
                    }
                }
                else
                {
                    if (ID.Contains(3))
                    {
                        ID.Remove(3);
                        NotifyPropertyChanged(nameof(Display3));
                    }
                }
            }
        }
        public bool Display4
        {
            get
            {
                return ID.Contains(4);
            }
            set
            {
                if (value)
                {
                    if (!ID.Contains(4))
                    {
                        ID.Add(4);
                        NotifyPropertyChanged(nameof(Display4));
                    }
                }
                else
                {
                    if (ID.Contains(4))
                    {
                        ID.Remove(4);
                        NotifyPropertyChanged(nameof(Display4));
                    }
                }
            }
        }
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
        public string Info2
        {
            get
            {
                string res = "";
                if (ID.Count != 0)
                {
                    var lst = ID.ToList();
                    lst.Sort();
                    foreach (int id in lst)
                    {
                        if (res != "") res += ",";
                        res += id.ToString();
                    }
                }
                return res.Trim();
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

        public BindingList<int> ID
        {
            get;
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
                    if (Delay == 0) return "Нет";
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
                    if (value == "Нет")
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
        public bool IsText
        {
            get
            {
                return !string.IsNullOrWhiteSpace(Text);
            }
        }
        public string Text
        {
            get
            {
                return mText;
            }
            set
            {
                mText = value;
                NotifyPropertyChanged(nameof(Text));
                NotifyPropertyChanged(nameof(IsText));
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
                NotifyPropertyChanged(nameof(IsNotLast));
                NotifyPropertyChanged(nameof(IsNotFirst));
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

        public TextStepModel(JToken json, DIR_TYPES? route)
        {
            var lst = new List<int>();
            if (json["id"] != null)
            {
                foreach (var itm in json["id"].Children())
                {
                    if (!lst.Contains((int)itm)) lst.Add((int)itm);
                }
                lst.Sort();
                ID = new BindingList<int>(lst);
            }
            else ID = new BindingList<int>();
            ID.ListChanged += ID_ListChanged;
            if (json["text"] != null) Text = (string)json["text"];
            if (json["delay"] != null) Delay = (int)json["delay"];
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
            if (ID.Count != 0)
            {
                JArray ids = new JArray();
                var lst = ID.ToList();
                lst.Distinct();
                lst.Sort();
                foreach (int id in lst)
                {
                    ids.Add(id);
                }
                res["id"] = ids;
            }
            res["text"] = Text;
            if (Delay != 0) res["delay"] = Delay;
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

        public TextStepModel(DIR_TYPES? route)
        {
            ID = new BindingList<int>();
            if (route != null)
            {
                Route = (DIR_TYPES)route;
                mStop = true;
            }
        }
        public TextStepModel(TextStepModel step)
        {
            ID = new BindingList<int>();
            Route = step.Route;
            mStop = step.mStop;
            GPS = step.GPS;
        }

        private void ID_ListChanged(object sender, ListChangedEventArgs e)
        {
            NotifyPropertyChanged(nameof(Info2));
        }

        public override string ToString()
        {
            string str = "{" + mListNum.ToString() + ":" + Text;
            if (ID.Count != 0)
            {
                str += "(";
                foreach (int i in ID)
                {
                    str += i.ToString();
                    if (i != ID.Last()) str += ",";
                }
                str += ")";
            }
            if (GPS != GPS_LEVEL.NONE) str += ";" + GPS.ToString();
            if (Route != DIR_TYPES.ANY) str += ";" + Route.ToString();
            if (Delay != 0.0) str += ";" + Delay.ToString() + "sec";
            if (IsNotFirst) str += "<";
            if (IsNotLast) str += ">";
            str += "}";
            return str;
        }
    }
}
