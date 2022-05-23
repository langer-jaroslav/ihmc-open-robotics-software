 #define centerX 0
 #define centerY 1
 #define r00 2
 #define r01 3
 #define r02 4
 #define r10 5
 #define r11 6
 #define r12 7
 #define r20 8
 #define r21 9
 #define r22 10
 #define tx 11
 #define ty 12
 #define tz 13

#define WIDTH 0
#define HEIGHT 1
#define RESOLUTION 2
#define MIN_VALID_DISTANCE 3
#define MAX_HEIGHT_RANGE 4
#define RAMPED_HEIGHT_RANGE_A 5
#define RAMPED_HEIGHT_RANGE_B 6
#define RAMPED_HEIGHT_RANGE_C 7
#define SENSOR_NOISE_FACTOR 8
#define INITIAL_VARIANCE 9
#define MAX_VARIANCE 10

#define HEIGHT_LAYER 0
#define VARIANCE_LAYER 1
#define POINT_COUNTER_LAYER 2

int clamp_val(int x, int min_x, int max_x)
{
    return max(min(x, max_x), min_x);
}

float z_noise(float z, read_only float* params)
{
    return params[SENSOR_NOISE_FACTOR] * z * z;
}

float transform_p(float x, float y, float z, float r0, float r1, float r2, float t)
{
    return r0 * x + r1 * y + r2 * z + t;
}

// TODO switch this to use float3
float point_sensor_distance(float x, float y, float z, float sx, float sy, float sz)
{
    float d = (x - sx) * (x - sx);// + (y - sy) * (y - sy) + (z - sz) * (z - sz);
    return d;
}

int get_x_idx(float x, float center, read_only float* params)
{
    int i = (x - center)  / params[RESOLUTION] + 0.5 * params[WIDTH];
    //printf("x value %f leads to index %i\n", x, i);
    return clamp_val(i, 0, params[WIDTH] - 1);
}

int get_y_idx(float y, float center, read_only float* params)
{
    int i = (y - center) / params[RESOLUTION] + 0.5 * params[HEIGHT];
    //printf("y value %f leads to index %i\n", y, i);

    return clamp_val(i, 0, params[HEIGHT] - 1);
}

int get_idx_in_layer(int idx_x, int idx_y, read_only float* params)
{
    return params[WIDTH] * idx_x + idx_y;
}

int get_idx(float x, float y, float center_x, float center_y, read_only float* params)
{
    int idx_x = get_x_idx(x, center_x, params);
    int idx_y = get_y_idx(y, center_y, params);

 //   if (idx_x == 0) printf("got a point");
    return get_idx_in_layer(idx_x, idx_y, params);
}

int get_map_idx_in_layer(int idx_x, int idx_y, int layer_n, read_only float* params)
{
  //   if (idx == 0) printf("layer_n %d \n", layer_n);
    const int layer = params[WIDTH] * params[HEIGHT];
    return layer * layer_n + get_idx_in_layer(idx_x, idx_y, params);
}

int get_map_idx(int idx, int layer_n, read_only float* params)
{
  //  if (idx == 0) printf("layer_n %d \n", layer_n);
    const int layer = params[WIDTH] * params[HEIGHT];
    return layer * layer_n + idx;
}


bool is_inside(int idx_x, int idx_y, read_only float* params)
{
    int max_width = params[WIDTH];
    if (idx_x == -1 || idx_x == max_width)
    {
       // printf("Not inside from width, (%i,%i), max %i\n", idx_x,idx_y, max_width);
        return false;
    }
    int max_height = params[HEIGHT];
    if (idx_y == -1 || idx_y == max_height)
    {
        //printf("Not inside from height, (%i,%i), max %i\n", idx_x,idx_y, max_height);
        return false;
    }

   // printf("Actually inside! %i, (%i, %i)\n", idx, idx_x, idx_y);
    return true;
}

bool is_valid(float x, float y, float z, float sx, float sy, float sz, read_only float* params)
{
    float d = point_sensor_distance(x, y, z, sx, sy, sz);
    float min = 0.0;

    if (d < params[MIN_VALID_DISTANCE] * params[MIN_VALID_DISTANCE])
    {
        //printf("Not valid from distance");
        return false;
    }

    float dxy = max(sqrt(x * x + y * y) - params[RAMPED_HEIGHT_RANGE_B], min);

    if (z - sz > dxy * params[RAMPED_HEIGHT_RANGE_A] + params[RAMPED_HEIGHT_RANGE_C] || z - sz > params[MAX_HEIGHT_RANGE])
    {
        //printf("Not valid from ramp");
        return false;
    }

    return true;
}

// taken from http://suhorukov.blogspot.com/2011/12/opencl-11-atomic-operations-on-floating.html
void AtomicAdd_g_f(volatile __global float *source, const float operand)
{
    union
    {
        unsigned int intVal;
        float floatVal;
    } newVal;
    union
    {
        unsigned int intVal;
        float floatVal;
    } prevVal;

    do
    {
        prevVal.floatVal = *source;
        newVal.floatVal = prevVal.floatVal + operand;
    }
    while (atomic_cmpxchg((volatile __global unsigned int *)source, prevVal.intVal, newVal.intVal) != prevVal.intVal);
}


void kernel zeroValuesKernel(global float* params,  global float* newMap)
{
    int idx_x = get_global_id(0);
    int idx_y = get_global_id(1);

    int height_idx = get_map_idx_in_layer(idx_x, idx_y, HEIGHT_LAYER, params);
    int variance_idx = get_map_idx_in_layer(idx_x, idx_y, VARIANCE_LAYER, params);
    int counter_idx = get_map_idx_in_layer(idx_x, idx_y, POINT_COUNTER_LAYER, params);

    newMap[height_idx] = 0.0;
    newMap[variance_idx] = 0.0;
    newMap[counter_idx] = 0.0;
}

void kernel addPointsKernel(global float* points_in, global float* localization, global float* params,  global float* newMap)
{
    int i = get_global_id(0);

    float rx = points_in[i * 3];
    float ry = points_in[i * 3 + 1];
    float rz = points_in[i * 3 + 2];

    float x = transform_p(rx, ry, rz, localization[r00], localization[r01], localization[r02], localization[tx]);
    float y = transform_p(rx, ry, rz, localization[r10], localization[r11], localization[r12], localization[ty]);
    float z = transform_p(rx, ry, rz, localization[r20], localization[r21], localization[r22], localization[tz]);

//    if (i == 0) printf("point %f, %f, %f\n", rx, ry, rz);

//    if (i == 0) printf("transformed point %f, %f, %f\n", x, y, z);

//    if (i == 0) printf("Size %f x %f\n", params[WIDTH], params[HEIGHT]);

  //  float v = z_noise(rz, params);

    if (is_valid(x, y, z, localization[tx], localization[ty], localization[tz], params))
    {
        int idx_x = get_x_idx(x, localization[centerX], params);
        int idx_y = get_y_idx(y, localization[centerY], params);
        int idx = get_idx_in_layer(idx_x, idx_y, params);

        // TODO pretty sure this is always true by construction
        if (is_inside(idx_x, idx_y, params))
        {
            int height_idx = get_map_idx(idx, HEIGHT_LAYER, params);
            int variance_idx = get_map_idx(idx, VARIANCE_LAYER, params);
            int counter_idx = get_map_idx(idx, POINT_COUNTER_LAYER, params);

            //printf("Indices %i, %i x %i, %i\n", idx, height_idx, variance_idx, counter_idx);

            //float map_h = map[height_idx];
            //float map_v = map[variance_idx];

            float new_height = z; //(map_h * v + z * map_v) / (map_v + v);
            float new_variance = z * z; //(map_v * v) / (map_v + v);

 //           printf("Adding height %f\n", new_height);
            AtomicAdd_g_f(&newMap[height_idx], new_height);
            AtomicAdd_g_f(&newMap[variance_idx], new_variance);
            AtomicAdd_g_f(&newMap[counter_idx], 1.0);


            // visibility cleanup
        }
    }
}

void kernel averageMapKernel(global float* newMap, global float* params)
{
    int idx_x = get_global_id(0);
    int idx_y = get_global_id(1);

    int height_idx = get_map_idx_in_layer(idx_x, idx_y, HEIGHT_LAYER, params);
    int variance_idx = get_map_idx_in_layer(idx_x, idx_y, VARIANCE_LAYER, params);

    //int height_idx = get_map_idx(idx_x, HEIGHT_LAYER, params);
    //int variance_idx = get_map_idx(idx_x, VARIANCE_LAYER, params);

    float new_h = newMap[height_idx];
    float new_v = newMap[variance_idx];
    float new_cnt = newMap[get_map_idx_in_layer(idx_x, idx_y, POINT_COUNTER_LAYER, params)];
   // float new_cnt = newMap[get_map_idx(idx_x, POINT_COUNTER_LAYER, params)];

 //   if (idx == 0) printf("width %i, height %i\n", params[WIDTH], params[HEIGHT]);

    if (new_cnt > 0)
    {
  //      printf("height index = %i,  variance index = %i \n", height_idx, variance_idx);

        // too much variance, say it's not valid
        // FIXME this only works when iteratively updating the scans
//        if (new_v / new_cnt > params[MAX_VARIANCE])
        {
//            newMap[height_idx] = 1.0;
//            newMap[variance_idx] = params[INITIAL_VARIANCE];
        }
//        else
        {
            newMap[height_idx] = new_h / new_cnt;
            newMap[variance_idx] = (new_v - new_h * new_h / new_cnt) / (new_cnt - 1);
        }
    }
}





