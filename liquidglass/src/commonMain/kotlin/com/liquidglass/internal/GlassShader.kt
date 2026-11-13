package com.liquidglass.internal

internal const val MaxGlassShapes = 8

private const val ShapeFunctions = """
float sdShape(float2 q, float2 hs, float r, float e) {
    float2 c = abs(q) - (hs - float2(r));
    if (c.x > 0.0 && c.y > 0.0) {
        if (e == 2.0) {
            return length(c) - r;
        }
        float l = pow(pow(c.x, e) + pow(c.y, e), 1.0 / e);
        float2 g = pow(c / l, float2(e - 1.0));
        return (l - r) * inversesqrt(dot(g, g));
    }
    return max(c.x, c.y) - r;
}

float2 shapeNormal(float2 q, float2 hs, float rn, float e) {
    float2 c = abs(q) - (hs - float2(rn));
    float2 g;
    if (c.x > 0.0 && c.y > 0.0) {
        g = e == 2.0 ? c : pow(c, float2(e - 1.0));
    } else {
        g = c.x > c.y ? float2(1.0, 0.0) : float2(0.0, 1.0);
    }
    return normalize(g) * sign(q);
}

float radiusAt(float2 q, float4 r) {
    return q.x >= 0.0 ? (q.y <= 0.0 ? r.y : r.z) : (q.y <= 0.0 ? r.x : r.w);
}
"""

private const val CommonUniforms = """
uniform shader content;
uniform float refractionAmount;
uniform float dispersion;
uniform float3 colorParams;
uniform float4 surface;
uniform float2 lightDir;
uniform float specular;
uniform float specularWidth;
uniform float2 innerShadow;
uniform float magnification;
"""

private const val Shading = """
const half3 LUMA = half3(0.2126, 0.7152, 0.0722);

half3 grade(half4 c, out half a) {
    a = c.a;
    half3 col = a > 0.0 ? c.rgb / a : half3(0.0);
    col = (col - 0.5) * half(colorParams.z) + 0.5 + half(colorParams.y);
    half lum = dot(col, LUMA);
    return mix(half3(lum), col, half(colorParams.x));
}

half3 applyTint(half3 col, half4 tc) {
    if (tc.a <= 0.0) {
        return col;
    }
    half lum = dot(col, LUMA);
    half3 tinted = clamp(tc.rgb * (0.82 + 0.3 * lum) + (lum - 0.5) * 0.12, 0.0, 1.0);
    return mix(col, tinted, clamp(tc.a, 0.0, 1.0));
}

half4 compose(half3 col, half a, float depth, float2 nn, float t, float glowAmount, bool inBand) {
    col = clamp(col, 0.0, 1.0);
    half3 pm = half3(surface.rgb) + col * a * (1.0 - half(surface.a));
    a = half(surface.a) + a * (1.0 - half(surface.a));
    float spec = glowAmount;
    if (inBand) {
        if (innerShadow.x > 0.0) {
            float shade = innerShadow.x * (1.0 - smoothstep(0.0, innerShadow.y, depth)) * (0.55 + 0.45 * max(-nn.y, 0.0));
            pm *= half(1.0 - shade);
        }
        float ndl = dot(nn, lightDir);
        float front = max(ndl, 0.0);
        float back = max(-ndl, 0.0) * 0.65;
        float rim = 1.0 - smoothstep(0.0, specularWidth, depth);
        float x = 1.0 - t;
        float bevel = x * x * x * x;
        spec += specular * (rim * (0.22 + 0.78 * (front + back)) + bevel * 0.1 * front);
    }
    a = min(a + half(spec), 1.0);
    pm = min(pm + half3(spec), half3(a));
    return half4(pm, a);
}

float displacement(float depth, float h) {
    float t = clamp(depth / h, 0.0, 1.0);
    float amount = min(refractionAmount, 0.62 * h);
    float ap = amount / h;
    float m = 1.0 / (1.0 - ap);
    float g = (3.0 - m) * t * t + (m - 2.0) * t * t * t;
    return max((ap + (1.0 - ap) * g - t) * h, 0.0);
}

half4 sampleRefracted(float2 sp, float2 nn, float disp) {
    half4 c = content.eval(sp);
    if (dispersion > 0.0 && disp > 0.5) {
        float2 dd = nn * (disp * dispersion * (disp / max(refractionAmount, 0.001)));
        half4 sr = content.eval(sp + dd);
        half4 sb = content.eval(sp - dd);
        if (sr.a > 0.001) {
            c.r = sr.r * (c.a / sr.a);
        }
        if (sb.a > 0.001) {
            c.b = sb.b * (c.a / sb.a);
        }
    }
    return c;
}
"""

internal const val SingleGlassShaderSource = CommonUniforms + """
uniform float4 rect;
uniform float4 radii;
uniform float3 shapeParams;
uniform float4 tint;
uniform float4 glow;
""" + ShapeFunctions + Shading + """
half4 main(float2 p) {
    float2 q = p - rect.xy;
    float2 hs = rect.zw;
    float e = shapeParams.x;
    float h = shapeParams.y;
    float band = shapeParams.z;
    float r = radiusAt(q, radii);
    float d = sdShape(q, hs, r, e);
    float coverage = clamp(0.5 - d, 0.0, 1.0);
    if (coverage <= 0.0) {
        return half4(0.0);
    }
    float depth = -d;
    float glowAmount = glow.w > 0.0 ? glow.w * smoothstep(glow.z, 0.0, distance(p, glow.xy)) : 0.0;
    float2 sp = rect.xy + q / magnification;
    half a;
    if (depth >= band) {
        half3 col = applyTint(grade(content.eval(sp), a), half4(tint));
        return compose(col, a, depth, float2(0.0), 1.0, glowAmount, false) * half(coverage);
    }
    float rn = min(max(r, h), min(hs.x, hs.y));
    float2 nn = shapeNormal(q, hs, rn, e);
    float disp = h > 0.0 ? displacement(depth, h) : 0.0;
    half3 col = applyTint(grade(sampleRefracted(sp - nn * disp, nn, disp), a), half4(tint));
    float t = h > 0.0 ? clamp(depth / h, 0.0, 1.0) : 1.0;
    return compose(col, a, depth, nn, t, glowAmount, true) * half(coverage);
}
"""

internal const val GlassShaderSource = CommonUniforms + """
uniform int count;
uniform float4 rects[8];
uniform float4 radii[8];
uniform float4 extras[8];
uniform float4 tints[8];
uniform float4 glows[8];
uniform float smoothing;
uniform float band;
""" + ShapeFunctions + Shading + """
half4 main(float2 p) {
    float k = smoothing;
    float d = 100000.0;
    float4 tint = float4(0.0);
    float tintRef = 100000.0;
    float glowAmount = 0.0;
    float2 ctr = float2(0.0);
    for (int i = 0; i < 8; i++) {
        if (i >= count) {
            break;
        }
        float2 q = p - rects[i].xy;
        float di = sdShape(q, rects[i].zw, radiusAt(q, radii[i]), extras[i].x);
        float m = i == 0 ? 1.0 : (k > 0.0 ? clamp(0.5 + 0.5 * (d - di) / k, 0.0, 1.0) : (di < d ? 1.0 : 0.0));
        d = i == 0 ? di : mix(d, di, m) - k * m * (1.0 - m);
        ctr = mix(ctr, rects[i].xy, m);
        float mt = clamp(1.0 + (tintRef - di) / max(0.5 * k, 1.0), 0.0, 1.0);
        tint = mix(tint, tints[i], mt);
        tintRef = min(tintRef, di);
        float4 gl = glows[i];
        if (gl.w > 0.0 && di < k) {
            glowAmount += gl.w * smoothstep(gl.z, 0.0, distance(p, gl.xy));
        }
    }
    float coverage = clamp(0.5 - d, 0.0, 1.0);
    if (coverage <= 0.0) {
        return half4(0.0);
    }
    float depth = -d;
    float2 sp = ctr + (p - ctr) / magnification;
    half a;
    if (depth >= band) {
        half3 col = applyTint(grade(content.eval(sp), a), half4(tint));
        return compose(col, a, depth, float2(0.0), 1.0, glowAmount, false) * half(coverage);
    }
    float d2 = 100000.0;
    float2 n = float2(0.0);
    float h = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= count) {
            break;
        }
        float2 q = p - rects[i].xy;
        float2 hs = rects[i].zw;
        float e = extras[i].x;
        float hi = extras[i].y;
        float r = radiusAt(q, radii[i]);
        float di = sdShape(q, hs, r, e);
        float2 ni = shapeNormal(q, hs, min(max(r, hi), min(hs.x, hs.y)), e);
        float m = i == 0 ? 1.0 : (k > 0.0 ? clamp(0.5 + 0.5 * (d2 - di) / k, 0.0, 1.0) : (di < d2 ? 1.0 : 0.0));
        d2 = i == 0 ? di : mix(d2, di, m) - k * m * (1.0 - m);
        n = mix(n, ni, m);
        h = mix(h, hi, m);
    }
    float nl = length(n);
    float2 nn = nl > 0.0001 ? n / nl : float2(0.0);
    float disp = h > 0.0 ? displacement(depth, h) : 0.0;
    half3 col = applyTint(grade(sampleRefracted(sp - nn * disp, nn, disp), a), half4(tint));
    float t = h > 0.0 ? clamp(depth / h, 0.0, 1.0) : 1.0;
    return compose(col, a, depth, nn, t, glowAmount, true) * half(coverage);
}
"""

internal const val GlassShadowShaderSource = """
uniform int count;
uniform float4 rects[8];
uniform float4 radii[8];
uniform float4 extras[8];
uniform float smoothing;
uniform float4 shadow;
""" + ShapeFunctions + """
half4 main(float2 p) {
    float2 sp = p - shadow.xy;
    float d = 100000.0;
    float k = smoothing;
    for (int i = 0; i < 8; i++) {
        if (i >= count) {
            break;
        }
        float2 q = sp - rects[i].xy;
        float di = sdShape(q, rects[i].zw, radiusAt(q, radii[i]), extras[i].x);
        float m = i == 0 ? 1.0 : (k > 0.0 ? clamp(0.5 + 0.5 * (d - di) / k, 0.0, 1.0) : (di < d ? 1.0 : 0.0));
        d = i == 0 ? di : mix(d, di, m) - k * m * (1.0 - m);
    }
    return half4(0.0, 0.0, 0.0, shadow.w * (1.0 - smoothstep(-0.35 * shadow.z, shadow.z, d)));
}
"""
